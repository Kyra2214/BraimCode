package com.brain.router

import org.json.JSONArray
import org.json.JSONObject

/**
 * Carrega o catálogo real reaproveitado do IaBrain (assets/ai_api_catalog.json —
 * 11 provedores gratuitos: DeepSeek, Qwen, Moonshot, Z.ai, MiniMax, StepFun,
 * Volcengine, SiliconFlow, ModelScope, Tencent, OpenRouter).
 *
 * O JSON não traz janela de limite (rate limit) por modelo — só id, nome,
 * capabilities e endpoint. Isso é intencional pro desenho do Brain: quem
 * manda na decisão é o LiveStats observado em tempo real (ver ApiCatalog.kt),
 * não uma janela estática declarada. JanelaLimite fica neutra (nulls) até
 * existir esse dado real (Fase G).
 */
object ApiCatalogLoader {

    fun fromJson(json: String): List<ProviderModel> {
        val root = JSONObject(json)
        val providers = root.getJSONArray("providers")
        val resultado = mutableListOf<ProviderModel>()

        for (i in 0 until providers.length()) {
            val provider = providers.getJSONObject(i)
            val providerId = provider.getString("id")
            val models = provider.optJSONArray("models") ?: continue

            for (j in 0 until models.length()) {
                val modelo = models.getJSONObject(j)
                resultado += ProviderModel(
                    providerId = providerId,
                    modeloId = modelo.getString("id"),
                    papeisSugeridos = papeisPara(modelo.optJSONArray("capabilities")),
                    janela = JanelaLimite(),
                    contextoMaximoTokens = null
                )
            }
        }
        return resultado
    }

    /**
     * Heurística inicial de mapeamento capability -> papel do pipeline.
     * TODO (Fase D/G): substituir por dado real de uso (ExperienceMemory)
     * em vez de adivinhar a partir da string de capability declarada.
     */
    private fun papeisPara(capabilities: JSONArray?): List<PapelPipeline> {
        if (capabilities == null || capabilities.length() == 0) return PapelPipeline.values().toList()
        val tags = (0 until capabilities.length()).map { capabilities.getString(it).lowercase() }.toSet()

        val papeis = mutableSetOf<PapelPipeline>()
        if ("reasoning" in tags) papeis += PapelPipeline.PLANEJAMENTO
        if ("chat" in tags) papeis += PapelPipeline.ESCRITA_DE_PROMPT
        if ("coding" in tags || "agent" in tags) papeis += PapelPipeline.EXECUCAO_CODIGO

        return if (papeis.isEmpty()) PapelPipeline.values().toList() else papeis.toList()
    }
}
