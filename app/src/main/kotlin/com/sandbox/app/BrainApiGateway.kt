package com.sandbox.app

import com.brain.provider.ProviderClient
import com.brain.provider.ProviderDispatcher
import com.brain.provider.ProviderRequest
import com.brain.provider.ProviderResponse
import com.brain.router.ApiCatalogRegistry
import com.brain.router.DefaultAIRouter
import com.brain.router.DynamicFreeApiCatalog
import com.brain.router.PapelPipeline
import com.brain.router.ProviderModel
import com.brain.router.RoutingDecision
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ponte real Brain -> APIs gratuitas.
 * O Brain escolhe provider/modelo; esta camada executa e percorre as
 * alternativas quando uma API falha, perde o modelo ou atinge limite.
 */
class BrainApiGateway(
    private val providers: List<ApiProvider>,
    private val keyStore: ApiKeyStore,
    private val router: DefaultAIRouter = DefaultAIRouter()
) {
    data class GatewayResult(
        val text: String,
        val providerId: String,
        val modelId: String,
        val attempts: List<String>
    )

    fun complete(prompt: String, papel: PapelPipeline = PapelPipeline.ESCRITA_DE_PROMPT): GatewayResult {
        require(prompt.isNotBlank()) { "prompt não pode ser vazio" }
        val catalog = ApiCatalogRegistry.current() ?: error("Catálogo de APIs não instalado")
        val dynamic = catalog as? DynamicFreeApiCatalog
        val attempts = mutableListOf<String>()
        val tried = mutableSetOf<String>()

        fun decide(): RoutingDecision? = router.decidir(papel, catalog, emptyList())

        val decision = decide() ?: error("Nenhuma API gratuita disponível para o papel $papel")
        val ordered = ArrayDeque<ProviderModel>()
        ordered.add(decision.escolhido)
        ordered.addAll(decision.alternativas)

        while (ordered.isNotEmpty()) {
            var model = ordered.removeFirst()
            val key = "${model.providerId}::${model.modeloId}"
            if (!tried.add(key)) continue

            // Atualiza somente o provider que está prestes a ser usado.
            val currentModels = dynamic?.refreshProvider(model.providerId).orEmpty()
            val refreshed = currentModels.firstOrNull { it.modeloId == model.modeloId }
            if (refreshed != null) {
                model = refreshed
            } else if (currentModels.isNotEmpty()) {
                val replacement = currentModels.firstOrNull { papel in it.papeisSugeridos }
                if (replacement != null) {
                    val replacementKey = "${replacement.providerId}::${replacement.modeloId}"
                    if (tried.add(replacementKey)) model = replacement
                    else {
                        attempts += "$key: modelo removido/indisponível"
                        continue
                    }
                } else {
                    attempts += "$key: modelo removido/indisponível"
                    continue
                }
            }

            val provider = providers.firstOrNull { it.id == model.providerId }
            if (provider == null) {
                attempts += "$key: provider não cadastrado"
                continue
            }
            val apiKey = keyStore.get(model.providerId)?.takeIf { it.isNotBlank() }
            if (apiKey == null) {
                attempts += "$key: chave não cadastrada"
                continue
            }
            val baseEndpoint = provider.models.firstOrNull()?.endpoint
            if (baseEndpoint.isNullOrBlank()) {
                attempts += "$key: endpoint ausente"
                continue
            }

            val client = AndroidProviderClient(
                providerId = model.providerId,
                endpoint = chatCompletionsEndpoint(baseEndpoint)
            )
            val request = ProviderRequest(
                model = model.modeloId,
                prompt = prompt,
                headers = mapOf("Authorization" to "Bearer $apiKey")
            )
            val response = ProviderDispatcher(catalog).dispatch(model, client, request).getOrNull()

            if (response != null && response.statusCode in 200..299) {
                val text = extractText(response.body)
                if (text.isNotBlank()) return GatewayResult(text, model.providerId, model.modeloId, attempts)
                attempts += "$key: resposta sem conteúdo"
            } else {
                attempts += "$key: HTTP ${response?.statusCode ?: 0}"
            }
        }

        throw IllegalStateException("Todas as APIs gratuitas falharam. Tentativas: ${attempts.joinToString(" | ")}")
    }

    private fun chatCompletionsEndpoint(base: String): String {
        val normalized = base.trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) normalized else "$normalized/chat/completions"
    }

    private fun extractText(body: String): String = runCatching {
        val root = JSONObject(body)
        val choices = root.optJSONArray("choices") ?: return@runCatching ""
        val first = choices.optJSONObject(0) ?: return@runCatching ""
        first.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
            ?: first.optString("text").takeIf { it.isNotBlank() }
            ?: ""
    }.getOrDefault("")
}

/** Cliente HTTP Android sem java.net.http, compatível com o runtime do APK. */
private class AndroidProviderClient(
    private val providerId: String,
    private val endpoint: String,
    private val timeoutMs: Int = 30_000
) : ProviderClient {
    override fun complete(request: ProviderRequest): Result<ProviderResponse> = runCatching {
        require(request.model.isNotBlank()) { "model não pode ser vazio" }
        require(request.prompt.isNotBlank()) { "prompt não pode ser vazio" }
        val started = System.nanoTime()
        val payload = JSONObject().apply {
            put("model", request.model)
            put("messages", org.json.JSONArray().put(JSONObject().put("role", "user").put("content", request.prompt)))
        }.toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            request.headers.filterKeys { it.lowercase() !in setOf("host", "content-length") }
                .forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            ProviderResponse(status, body, (System.nanoTime() - started) / 1_000_000, providerId)
        } finally {
            connection.disconnect()
        }
    }
}
