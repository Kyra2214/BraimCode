package com.sandbox.app

import com.brain.provider.HttpProviderClient
import com.brain.provider.ProviderDispatcher
import com.brain.provider.ProviderRequest
import com.brain.router.ApiCatalogRegistry
import com.brain.router.DefaultAIRouter
import com.brain.router.DynamicFreeApiCatalog
import com.brain.router.PapelPipeline
import com.brain.router.ProviderModel
import com.brain.router.RoutingDecision
import com.brain.router.RoutingProfile
import org.json.JSONObject
import java.net.URI

/**
 * Ponte real Brain -> APIs gratuitas.
 *
 * O Brain decide o provider/modelo; esta camada só traduz a decisão para a
 * API OpenAI-compatible, executa e percorre as alternativas em caso de
 * limite, chave inválida, modelo removido ou erro HTTP.
 */
class BrainApiGateway(
    private val providers: List<ApiProvider>,
    private val keyStore: ApiKeyStore,
    private val router: DefaultAIRouter = DefaultAIRouter()
) {
    data class Result(
        val text: String,
        val providerId: String,
        val modelId: String,
        val attempts: List<String>
    )

    fun complete(prompt: String, papel: PapelPipeline = PapelPipeline.ESCRITA_DE_PROMPT): Result<Result> {
        require(prompt.isNotBlank()) { "prompt não pode ser vazio" }
        val catalog = ApiCatalogRegistry.current() ?: error("Catálogo de APIs não instalado")
        val dynamic = catalog as? DynamicFreeApiCatalog
        val attempts = mutableListOf<String>()
        val tried = mutableSetOf<String>()

        fun decide(): RoutingDecision? = router.decidir(papel, catalog, emptyList())

        var decision = decide() ?: error("Nenhuma API gratuita disponível para o papel $papel")
        val ordered = ArrayDeque<ProviderModel>()
        ordered.add(decision.escolhido)
        ordered.addAll(decision.alternativas)

        while (ordered.isNotEmpty()) {
            var model = ordered.removeFirst()
            val key = "${model.providerId}::${model.modeloId}"
            if (!tried.add(key)) continue

            // Atualiza somente a API que está prestes a ser usada.
            val currentModels = dynamic?.refreshProvider(model.providerId).orEmpty()
            val refreshed = currentModels.firstOrNull { it.modeloId == model.modeloId }
            if (refreshed != null) model = refreshed
            else if (currentModels.isNotEmpty()) {
                val replacement = currentModels.firstOrNull { papel in it.papeisSugeridos }
                if (replacement != null && tried.add("${replacement.providerId}::${replacement.modeloId}")) {
                    model = replacement
                } else {
                    attempts += "$key: modelo removido/indisponível"
                    continue
                }
            }

            val provider = providers.firstOrNull { it.id == model.providerId }
            val apiKey = keyStore.get(model.providerId)?.takeIf { it.isNotBlank() }
            if (provider == null) {
                attempts += "$key: provider não cadastrado"
                continue
            }
            if (apiKey == null) {
                attempts += "$key: chave não cadastrada"
                continue
            }

            val baseEndpoint = provider.models.firstOrNull()?.endpoint
                ?: run {
                    attempts += "$key: endpoint ausente"
                    continue
                }
            val endpoint = chatCompletionsEndpoint(baseEndpoint)
            val client = HttpProviderClient(model.providerId, URI(endpoint))
            val request = ProviderRequest(
                model = model.modeloId,
                prompt = prompt,
                headers = mapOf("Authorization" to "Bearer $apiKey")
            )
            val response = ProviderDispatcher(catalog).dispatch(model, client, request).getOrNull()

            if (response != null && response.statusCode in 200..299) {
                val text = extractText(response.body)
                if (text.isNotBlank()) {
                    return Result(text, model.providerId, model.modeloId, attempts)
                }
                attempts += "$key: resposta sem conteúdo"
            } else {
                val status = response?.statusCode ?: 0
                attempts += "$key: HTTP $status"
            }
        }

        // Uma nova decisão após as falhas permite que o LiveStats penalize os
        // providers quebrados antes de desistirmos da camada de APIs.
        decision = decide() ?: throw IllegalStateException(
            "Todas as APIs gratuitas falharam. Tentativas: ${attempts.joinToString(" | ")}" 
        )
        throw IllegalStateException("Todas as APIs gratuitas falharam. Tentativas: ${attempts.joinToString(" | ")}")
    }

    private fun chatCompletionsEndpoint(base: String): String {
        val normalized = base.trimEnd('/')
        return when {
            normalized.endsWith("/chat/completions") -> normalized
            else -> "$normalized/chat/completions"
        }
    }

    private fun extractText(body: String): String = runCatching {
        val root = JSONObject(body)
        val choices = root.optJSONArray("choices") ?: return@runCatching ""
        val first = choices.optJSONObject(0) ?: return@runCatching ""
        val message = first.optJSONObject("message")
        message?.optString("content")?.takeIf { it.isNotBlank() }
            ?: first.optString("text").takeIf { it.isNotBlank() }
            ?: ""
    }.getOrDefault("")
}
