package com.brain.core

import com.brain.provider.ProviderClient
import com.brain.provider.ProviderRequest
import org.json.JSONObject

/**
 * Secretário baseado em um provider local compatível com o contrato ProviderClient.
 * O provider pode apontar para llama.cpp/Ollama/Qwen local; nenhuma rede é aberta
 * por esta classe. Em falha, resposta inválida ou baixa confiança, usa o classificador
 * determinístico e mantém a decisão segura.
 */
class LocalLLMSecretario(
    private val provider: ProviderClient,
    private val model: String,
    private val fallback: Secretario = KeywordSecretario()
) : Secretario {
    override suspend fun interpretarPedido(request: UserRequest): ProjectIntent {
        val response = provider.complete(ProviderRequest(model, prompt(request.rawText))).getOrNull()
        val parsed = response
            ?.takeIf { it.statusCode in 200..299 }
            ?.let { parseIntent(it.body, request) }
        return parsed ?: fallback.interpretarPedido(request)
    }

    override suspend fun pesquisarParaClassificacao(intent: ProjectIntent): List<String> =
        fallback.pesquisarParaClassificacao(intent)

    override fun decidirEstrategia(intent: ProjectIntent): Estrategia =
        fallback.decidirEstrategia(intent)

    private fun prompt(text: String): String = """
        Classifique o pedido abaixo. Responda SOMENTE JSON válido com as chaves:
        projectType (string), platform (string ou null), complexity (BAIXA|MEDIA|ALTA),
        areasInvolvidas (array de strings), podeResolverLocal (boolean).
        Não execute comandos, não invente contexto e não inclua markdown.
        Pedido: ${text.take(8000)}
    """.trimIndent()

    private fun parseIntent(body: String, request: UserRequest): ProjectIntent? = runCatching {
        val root = JSONObject(body)
        val value = if (root.has("projectType")) root else
            root.optJSONObject("result") ?: root.optJSONObject("response") ?: return null
        val type = value.optString("projectType").trim()
        val complexity = runCatching {
            Complexity.valueOf(value.optString("complexity").uppercase())
        }.getOrNull() ?: return null
        val areas = value.optJSONArray("areasInvolvidas") ?: return null
        val parsedAreas = buildList { for (index in 0 until areas.length()) add(areas.optString(index)) }
            .filter { it.isNotBlank() }
        if (type.isBlank() || parsedAreas.size > 32) return null
        ProjectIntent(
            originalRequest = request,
            projectType = type,
            platform = value.optString("platform").takeIf { it.isNotBlank() && it != "null" },
            complexity = complexity,
            areasInvolvidas = parsedAreas,
            podeResolverLocal = value.optBoolean("podeResolverLocal", false)
        )
    }.getOrNull()
}
