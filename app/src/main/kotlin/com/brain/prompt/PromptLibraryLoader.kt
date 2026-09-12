package com.brain.prompt

import org.json.JSONArray
import org.json.JSONObject

/**
 * Carrega o seed real reaproveitado do IaBrain (assets/prompts_biblioteca.json,
 * 75 prompts já catalogados por categoria/subcaso/tags).
 *
 * O JSON tem campos (categoria, subcaso, tags, melhor_para) que não existem
 * como propriedades separadas em PromptTemplate — são dobrados dentro de
 * contextoDeUso como texto pesquisável, porque é isso que
 * InMemoryPromptLibrary.buscarPorContexto() usa pra achar template por
 * tokens, do mesmo jeito que o PromptLibraryService.requireTemplate()
 * original comparava tags com os tokens do pedido.
 */
object PromptLibraryLoader {

    private const val TAXA_SUCESSO_INICIAL = 0.6 // seed já curado, ainda sem uso real: neutro-otimista até haver dado real

    fun fromJson(json: String): List<PromptTemplate> {
        val root = JSONObject(json)
        val prompts = root.getJSONArray("prompts")
        val resultado = mutableListOf<PromptTemplate>()

        for (i in 0 until prompts.length()) {
            val item = prompts.getJSONObject(i)
            resultado += PromptTemplate(
                id = item.getString("id"),
                versao = 1,
                finalidade = item.optString("objetivo", item.optString("titulo", "")),
                contextoDeUso = contextoDeUso(item),
                skillRelacionada = null, // TODO (Fase E): ligar por categoria/subcaso quando o Catálogo de Skills existir
                agenteRelacionado = agentesRelacionados(item.optJSONArray("melhor_para")),
                textoTemplate = item.getString("template"),
                taxaSucesso = TAXA_SUCESSO_INICIAL,
                custoMedio = 0.0,  // provedores gratuitos, como o resto do catálogo
                tempoMedioMs = 0L, // sem dado real ainda; primeira leva de registrarResultado() ajusta
                historicoMelhorias = emptyList()
            )
        }
        return resultado
    }

    private fun contextoDeUso(item: JSONObject): String {
        val categoria = item.optString("categoria", "")
        val subcaso = item.optString("subcaso", "")
        val descricao = item.optString("descricao_curta", "")
        val tags = jsonArrayToList(item.optJSONArray("tags")).joinToString(" ")
        return listOf(categoria, subcaso, descricao, tags).filter { it.isNotBlank() }.joinToString(" | ")
    }

    private fun agentesRelacionados(melhorPara: JSONArray?): String? {
        val lista = jsonArrayToList(melhorPara)
        return lista.ifEmpty { null }?.joinToString(", ")
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }
}
