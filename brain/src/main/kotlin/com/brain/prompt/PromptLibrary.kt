package com.brain.prompt

/**
 * Reaproveitado do IaBrain (lá era "Biblioteca de Prompts", persistida
 * como PromptEntity no Room). Não é UI de vitrine — é o Banco de Prompts
 * operacional descrito no item 4 do plano original de Fase 2.
 *
 * Já existe SEED REAL reaproveitado do IaBrain em
 * app/src/main/assets/prompts_biblioteca.json — usar como carga inicial
 * da PromptLibrary em vez de começar vazio.
 *
 * Diferença para PromptGerado (em PromptGenerator.kt): PromptGerado é a
 * instância de um prompt para UMA tarefa específica. PromptTemplate é o
 * prompt reutilizável, com histórico de desempenho, que o PromptGenerator
 * deve consultar ANTES de gerar um novo do zero — reuso antes de criação,
 * mesmo princípio econômico do item 16 do plano original (memória e
 * conhecimento existente antes de gastar uma chamada de API nova).
 */
data class PromptTemplate(
    val id: String,
    val versao: Int,
    val finalidade: String,          // ex.: "gerar model de dados Room a partir de descrição de entidade"
    val contextoDeUso: String,       // em que tipo de tarefa/roadmap esse prompt se aplica
    val skillRelacionada: String?,
    val agenteRelacionado: String?,
    val textoTemplate: String,       // com placeholders a preencher por tarefa
    val taxaSucesso: Double,         // 0.0..1.0, atualizada via ExperienceMemory
    val custoMedio: Double,
    val tempoMedioMs: Long,
    val historicoMelhorias: List<String> = emptyList()
)

interface PromptLibrary {
    suspend fun buscarPorContexto(contextoDeUso: String): List<PromptTemplate>
    suspend fun salvarNovaVersao(template: PromptTemplate)

    /** Chamado depois de cada uso real, alimentado pela Fase D (Memória). */
    suspend fun registrarResultado(templateId: String, sucesso: Boolean, custo: Double, tempoMs: Long)
}

/**
 * Ponte síncrona opcional para componentes de decisão rápida como Retrieval.
 * Implementações persistentes podem preencher um snapshot sem mudar o
 * contrato suspend da biblioteca operacional.
 */
interface PromptLibrarySnapshot {
    fun buscarPorContextoSnapshot(contextoDeUso: String): List<PromptTemplate>
}
