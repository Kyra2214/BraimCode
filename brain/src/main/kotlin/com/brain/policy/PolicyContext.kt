package com.brain.policy

/**
 * Resultado possível de uma autorização. Espelha
 * reference/braincode-python/brain_runtime/models.py::Decision.
 */
enum class Decision { ALLOW, ASK, DENY }

/**
 * Nível de aprovação humana exigido antes de uma capacidade poder ser
 * executada, mesmo quando a Policy já autorizaria sozinha (decision = ASK).
 */
enum class ApprovalRequired { NONE, USER, ADMIN }

/**
 * Alias local para o RiskClass do contrato de execução — a Policy e o
 * Sandbox compartilham a mesma classificação de risco, não duplicam enums.
 */
typealias RiskClass = com.brain.execution.RiskClass

/**
 * O que o solicitante (Brain/Agente) declara antes de pedir autorização.
 * O PolicyBroker nunca confia nesses valores como fato consumado — ele os
 * valida e decide com base neles (deny-by-default).
 *
 * @property budget valores livres de orçamento (ex.: "cpu_ms", "output_bytes")
 *   — mapeamento genérico igual ao Python; a conversão para o
 *   [com.brain.execution.ResourceBudget] estruturado do contrato de
 *   execução acontece em [ExecutionAuthorization], não aqui.
 */
data class PolicyContext(
    val runId: String,
    val taskId: String,
    val actor: String,
    val riskClass: RiskClass = RiskClass.LOW,
    val approval: ApprovalRequired = ApprovalRequired.NONE,
    val sandboxRequired: Boolean = true,
    val networkAllowed: Boolean = false,
    val filesystemRoots: List<String> = emptyList(),
    val budget: Map<String, Long> = emptyMap(),
    val ttlSeconds: Int = 300,
    val expiresAt: String? = null
) {
    init {
        require(runId.isNotBlank()) { "runId não pode ser vazio" }
        require(taskId.isNotBlank()) { "taskId não pode ser vazio" }
        require(actor.isNotBlank()) { "actor não pode ser vazio" }
    }
}

/**
 * Registro imutável de uma decisão de autorização. Nunca é reescrito depois
 * de emitido — uma nova decisão é sempre uma nova instância, com novo
 * [decisionId]. Espelha PolicyDecision em
 * reference/braincode-python/brain_runtime/models.py.
 */
data class PolicyDecision(
    val decisionId: String,
    val runId: String,
    val taskId: String,
    val actor: String,
    val capability: String,
    val riskClass: RiskClass,
    val decision: Decision,
    val approvalRequired: ApprovalRequired,
    val sandboxRequired: Boolean,
    val networkAllowed: Boolean,
    val filesystemRoots: List<String>,
    val budget: Map<String, Long>,
    val expiresAt: String,
    val reason: String,
    val resource: String = ""
) {
    val isExpired: Boolean
        get() = java.time.Instant.now().isAfter(java.time.Instant.parse(expiresAt))
}
