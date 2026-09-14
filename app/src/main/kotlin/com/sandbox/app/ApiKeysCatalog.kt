package com.sandbox.app

import android.content.Context
import org.json.JSONObject

/**
 * Um modelo específico oferecido por um provider (granularidade de modelo,
 * não só de empresa — ver com.brain.router.ApiCatalog para o motivo).
 */
data class ApiProviderModel(
    val id: String,
    val name: String,
    val capabilities: List<String>,
    val access: String,
    val endpoint: String,
    val requiresKey: Boolean
)

data class ApiProvider(
    val id: String,
    val name: String,
    val region: String,
    val officialUrl: String,
    val documentationUrl: String,
    val models: List<ApiProviderModel>
)

/**
 * Carrega o catálogo de APIs que o agente pode usar.
 *
 * REGRA DO PROJETO: somente acesso gratuito entra no catálogo operacional.
 * São aceitos FREE_TIER e FREE_PERMANENT. Créditos promocionais e PAYG
 * ficam fora para impedir que o Brain roteie trabalho para uma API que possa
 * gerar cobrança.
 */
object ApiKeyCatalogLoader {
    private val FREE_ACCESS = setOf("FREE_TIER", "FREE_PERMANENT")

    fun load(context: Context): List<ApiProvider> {
        val json = context.assets.open("ai_api_catalog.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val providersJson = root.getJSONArray("providers")
        return buildList {
            for (index in 0 until providersJson.length()) {
                val providerObj = providersJson.getJSONObject(index)
                val modelsJson = providerObj.getJSONArray("models")
                val models = buildList {
                    for (modelIndex in 0 until modelsJson.length()) {
                        val modelObj = modelsJson.getJSONObject(modelIndex)
                        val access = modelObj.optString("access", "FREE_TIER")
                        if (access !in FREE_ACCESS) continue
                        val capabilitiesJson = modelObj.optJSONArray("capabilities")
                        val capabilities = buildList {
                            if (capabilitiesJson != null) {
                                for (capIndex in 0 until capabilitiesJson.length()) add(capabilitiesJson.getString(capIndex))
                            }
                        }
                        add(
                            ApiProviderModel(
                                id = modelObj.getString("id"),
                                name = modelObj.getString("name"),
                                capabilities = capabilities,
                                access = access,
                                endpoint = modelObj.getString("endpoint"),
                                requiresKey = modelObj.optBoolean("requiresKey", true)
                            )
                        )
                    }
                }
                if (models.isNotEmpty()) {
                    add(
                        ApiProvider(
                            id = providerObj.getString("id"),
                            name = providerObj.getString("name"),
                            region = providerObj.optString("region", ""),
                            officialUrl = providerObj.getString("officialUrl"),
                            documentationUrl = providerObj.optString("documentationUrl", providerObj.getString("officialUrl")),
                            models = models
                        )
                    )
                }
            }
        }
    }
}
