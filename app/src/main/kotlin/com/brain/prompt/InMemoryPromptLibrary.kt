package com.brain.prompt

import java.util.concurrent.ConcurrentHashMap

/**
 * Implementação mínima do PromptLibrary (Fase C, aprende de verdade na
 * Fase D). Carrega o seed real (ver PromptLibraryLoader) e resolve
 * buscarPorContexto() por sobreposição de tokens entre o contexto
 * pedido e o contextoDeUso salvo — mesma ideia de scoring por tags do
 * PromptLibraryService.requireTemplate() do IaBrain (brain/PromptGenerationFlow.kt),
 * só que devolvendo uma lista ranqueada em vez de exigir exatamente um
 * (o PromptGenerator decide o corte de taxaSucesso mínima, não a library).
 *
 * Ainda não persiste entre reinícios — isso é trabalho da Fase D
 * (memory): o ExperienceMemory registra a Experiencia completa, e essa
 * mesma taxaSucesso/custoMedio/tempoMedioMs aqui deve passar a vir de
 * lá, não só do contador em memória local.
 */
class InMemoryPromptLibrary(
    templatesIniciais: List<PromptTemplate>
) : PromptLibrary {

    private data class Contador(
        val sucesso: Int = 0,
        val falha: Int = 0,
        val custoTotal: Double = 0.0,
        val tempoTotalMs: Long = 0
    )

    private val templates = ConcurrentHashMap<String, PromptTemplate>().apply {
        templatesIniciais.forEach { put(it.id, it) }
    }
    private val contadores = ConcurrentHashMap<String, Contador>()

    override suspend fun buscarPorContexto(contextoDeUso: String): List<PromptTemplate> {
        val tokensPedido = tokenizar(contextoDeUso)
        if (tokensPedido.isEmpty()) return emptyList()

        return templates.values
            .map { template -> template to relevancia(template, tokensPedido) }
            .filter { (_, pontuacao) -> pontuacao > 0 }
            .sortedWith(
                compareByDescending<Pair<PromptTemplate, Int>> { it.second }
                    .thenByDescending { it.first.taxaSucesso }
            )
            .map { it.first }
    }

    override suspend fun salvarNovaVersao(template: PromptTemplate) {
        templates[template.id] = template
    }

    override suspend fun registrarResultado(templateId: String, sucesso: Boolean, custo: Double, tempoMs: Long) {
        val atual = templates[templateId] ?: return
        val contador = contadores.compute(templateId) { _, existente ->
            val base = existente ?: Contador()
            base.copy(
                sucesso = base.sucesso + if (sucesso) 1 else 0,
                falha = base.falha + if (sucesso) 0 else 1,
                custoTotal = base.custoTotal + custo,
                tempoTotalMs = base.tempoTotalMs + tempoMs
            )
        } ?: return

        val total = contador.sucesso + contador.falha
        templates[templateId] = atual.copy(
            taxaSucesso = contador.sucesso.toDouble() / total,
            custoMedio = contador.custoTotal / total,
            tempoMedioMs = contador.tempoTotalMs / total
        )
    }

    private fun relevancia(template: PromptTemplate, tokensPedido: Set<String>): Int {
        val tokensTemplate = tokenizar(template.contextoDeUso) + tokenizar(template.finalidade)
        return tokensPedido.count { token -> tokensTemplate.any { it.contains(token) || token.contains(it) } }
    }

    private fun tokenizar(texto: String): Set<String> =
        texto.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }
            .toSet()
}
