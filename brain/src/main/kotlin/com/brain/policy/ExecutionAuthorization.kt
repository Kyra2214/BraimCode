package com.brain.policy

import com.brain.execution.ResourceBudget

/**
 * O "terreno preparado" que o Brain entrega pro Sandbox antes de qualquer
 * agente trabalhar. Só existe a partir de uma [PolicyDecision] com
 * decision = ALLOW — não há construtor público, nem forma de montar uma
 * autorização "manualmente" a partir de ASK ou DENY. Isso é intencional:
 * nenhum caminho de código deve conseguir abrir uma sessão de Sandbox sem
 * ter passado pela Policy primeiro.
 *
 * A Etapa 3 (AgentSandboxSession, ainda não escrita) é quem consome isto:
 * `Sandbox.abrirSessao(autorizacao: ExecutionAuthorization): AgentSandboxSession`.
 */
class ExecutionAuthorization private constructor(
    val decisionId: String,
    val runId: String,
    val taskId: String,
    val actor: String,
    val capability: String,
    val riskClass: RiskClass,
    val networkAllowed: Boolean,
    val filesystemRoots: List<String>,
    val budget: ResourceBudget,
    val expiresAt: String
) {
    // Classe comum, não data class: um data class com construtor privado
    // ainda gera copy() público, que permitiria montar uma instância
    // "adulterada" sem passar pela Policy — exatamente o tipo de furo que
    // este tipo existe para evitar. equals/hashCode/toString abaixo são
    // escritos à mão só para comparação e debug em testes.

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExecutionAuthorization) return false
        return decisionId == other.decisionId && runId == other.runId && taskId == other.taskId &&
            actor == other.actor && capability == other.capability && riskClass == other.riskClass &&
            networkAllowed == other.networkAllowed && filesystemRoots == other.filesystemRoots &&
            budget == other.budget && expiresAt == other.expiresAt
    }

    override fun hashCode(): Int =
        listOf(decisionId, runId, taskId, actor, capability, riskClass, networkAllowed, filesystemRoots, budget, expiresAt).hashCode()

    override fun toString(): String =
        "ExecutionAuthorization(decisionId=$decisionId, runId=$runId, taskId=$taskId, actor=$actor, " +
            "capability=$capability, riskClass=$riskClass, networkAllowed=$networkAllowed, " +
            "filesystemRoots=$filesystemRoots, budget=$budget, expiresAt=$expiresAt)"

    companion object {
        /**
         * Única forma de obter uma ExecutionAuthorization. Retorna null
         * para ASK, DENY ou uma decisão já expirada — recusar é sempre
         * "sem terreno", nunca "terreno parcial".
         */
        fun fromDecision(decision: PolicyDecision): ExecutionAuthorization? {
            if (decision.decision != Decision.ALLOW) return null
            if (decision.isExpired) return null
            return ExecutionAuthorization(
                decisionId = decision.decisionId,
                runId = decision.runId,
                taskId = decision.taskId,
                actor = decision.actor,
                capability = decision.capability,
                riskClass = decision.riskClass,
                networkAllowed = decision.networkAllowed,
                filesystemRoots = decision.filesystemRoots,
                budget = decision.budget.toResourceBudget(),
                expiresAt = decision.expiresAt
            )
        }
    }
}

/**
 * Converte o budget genérico da Policy (chave livre -> valor, igual ao
 * Python) pro ResourceBudget estruturado do contrato de execução. Chaves
 * reconhecidas: cpu_ms, memory_bytes, output_bytes, artifact_bytes;
 * chaves fora dessa lista são ignoradas aqui.
 *
 * Nota: a Policy aceita budget 0 (só rejeita negativo); ResourceBudget
 * exige valor > 0 quando o campo não é nulo. Uma chave presente com valor
 * 0 hoje lança IllegalArgumentException nesta conversão — comportamento
 * aceitável por ora (0 de orçamento não autorizaria nada mesmo), mas fica
 * registrado para quando a Etapa 3 precisar decidir se 0 deve significar
 * "sem limite nesse eixo" em vez de erro.
 */
private fun Map<String, Long>.toResourceBudget(): ResourceBudget = ResourceBudget(
    maxCpuMillis = this["cpu_ms"],
    maxMemoryBytes = this["memory_bytes"],
    maxOutputBytes = this["output_bytes"],
    maxArtifactBytes = this["artifact_bytes"]
)
