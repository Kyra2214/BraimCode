package com.brain.execution

/**
 * Contrato Brain ↔ Sandbox — AGNÓSTICO DE IMPLEMENTAÇÃO.
 *
 * O Brain não sabe (nem deve saber) que comando de shell vai rodar,
 * qual RootFS existe ou como o Sandbox resolve o job internamente.
 * O Brain descreve OBJETIVO + REQUISITOS + CONTEXTO; o Sandbox decide
 * como executar e devolve um RESULTADO PADRONIZADO.
 *
 * Isso evita acoplar o cérebro ao formato específico de comando do
 * Sandbox de hoje — se o Sandbox mudar de proot para outra coisa
 * amanhã, o Brain não muda uma linha.
 *
 * ATENÇÃO: ainda é rascunho de trabalho — só vira implementação real
 * depois que os dois lados assinarem esse formato por escrito.
 */

data class Job(
    val tarefaId: String,
    val objetivo: String,              // o que precisa acontecer, em linguagem de domínio, não em comando
    val requisitos: List<Requisito>,   // capacidades necessárias, não binários específicos
    val contexto: JobContext,
    val timeoutSegundos: Int
)

/** Capacidade que o job precisa, não a ferramenta exata que a fornece. */
data class Requisito(
    val capacidade: String   // ex.: "compilar_android", "rodar_testes_unitarios", "runtime_node"
)

data class JobContext(
    val arquivosEntrada: List<String>,
    val variaveis: Map<String, String> = emptyMap()
)

enum class JobStatus { SUCESSO, FALHA, TIMEOUT, REQUISITO_INDISPONIVEL }

data class JobResult(
    val tarefaId: String,
    val status: JobStatus,
    val saida: String,             // log/relato humano-legível do que aconteceu
    val artefatos: List<String>,   // arquivos produzidos/alterados
    val tempoExecucaoMs: Long,
    val detalheErro: String? = null
)

/**
 * TODO: implementar só depois do contrato assinado e do Sandbox sair
 * do congelamento. A implementação concreta (proot, shell, o que for)
 * fica inteiramente do lado do Sandbox — esta interface não vaza isso.
 */
interface SandboxExecutor {
    suspend fun executar(job: Job): JobResult
}
