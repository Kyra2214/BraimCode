package com.brain.policy

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Autoridade única de autorização. Catálogos, routers e dispatchers nunca
 * autorizam por conta própria — todos passam por aqui.
 *
 * Porta 1:1 o comportamento de
 * reference/braincode-python/brain_runtime/policy.py::PolicyBroker,
 * incluindo a ordem de verificação (importa para os testes espelhados em
 * PolicyBrokerTest, que seguem os mesmos casos de
 * reference/braincode-python/tests/test_policy_events.py).
 *
 * Deny-by-default: qualquer combinação de actor/capability/contexto que não
 * passe explicitamente por todas as checagens abaixo é negada.
 */
class PolicyBroker(
    allowedCapabilities: Collection<String> = emptyList(),
    actorCapabilities: Map<String, Collection<String>> = emptyMap()
) {
    private val allowed: Set<String> = allowedCapabilities.toSet()
    private val actors: Map<String, Set<String>> = actorCapabilities.mapValues { it.value.toSet() }

    fun authorize(actor: String, capability: String, resource: String, context: PolicyContext): PolicyDecision {
        var decision = Decision.DENY
        var reason = "denied by default"

        when {
            context.ttlSeconds <= 0 ->
                reason = "policy TTL must be positive"

            context.budget.values.any { it < 0 } ->
                reason = "budget values cannot be negative"

            capability !in allowed ->
                reason = "capability '$capability' is not registered"

            capability !in (actors[actor] ?: emptySet()) ->
                reason = "actor '$actor' is not authorized for '$capability'"

            !context.sandboxRequired && context.riskClass !in setOf(RiskClass.LOW, RiskClass.READ_ONLY) ->
                reason = "sandbox is mandatory for non-low-risk capability"

            capability.startsWith("network") && !context.networkAllowed ->
                reason = "network access is not allowed by policy"

            capability.startsWith("filesystem") && context.filesystemRoots.isEmpty() ->
                reason = "filesystem capability requires an explicit root"

            context.expiresAt != null && !Instant.now().isBefore(Instant.parse(context.expiresAt)) ->
                reason = "policy context has expired"

            context.approval != ApprovalRequired.NONE -> {
                decision = Decision.ASK
                reason = "explicit ${context.approval} approval is required"
            }

            else -> {
                decision = Decision.ALLOW
                reason = "registered capability authorized for resource '$resource'"
            }
        }

        val expires = Instant.now().plus(maxOf(1, context.ttlSeconds).toLong(), ChronoUnit.SECONDS)
        return PolicyDecision(
            decisionId = "decision_${UUID.randomUUID()}",
            runId = context.runId,
            taskId = context.taskId,
            actor = actor,
            capability = capability,
            riskClass = context.riskClass,
            decision = decision,
            approvalRequired = context.approval,
            sandboxRequired = context.sandboxRequired,
            networkAllowed = context.networkAllowed,
            filesystemRoots = context.filesystemRoots,
            budget = context.budget,
            expiresAt = expires.toString(),
            reason = reason,
            resource = resource
        )
    }

    /**
     * Confirma que uma decisão já emitida ainda vale — ALLOW, não expirada
     * e (quando o recurso é um path) dentro de um dos filesystemRoots
     * autorizados. Nunca reavalia a Policy; só valida o que já foi decidido.
     */
    fun check(decision: PolicyDecision, resource: String? = null): Boolean {
        if (decision.decision != Decision.ALLOW || decision.isExpired) return false
        if (resource != null && decision.filesystemRoots.isNotEmpty()) {
            val path = java.io.File(resource).canonicalFile
            val dentroDeAlgumRoot = decision.filesystemRoots.any { root ->
                val rootFile = java.io.File(root).canonicalFile
                path == rootFile || path.toPath().startsWith(rootFile.toPath())
            }
            if (!dentroDeAlgumRoot) return false
        }
        return true
    }

    /** Retorna um novo PolicyBroker (imutável) com a capacidade adicionada para o actor. */
    fun withActorCapability(actor: String, capability: String): PolicyBroker {
        val novosActors = actors.mapValues { it.value.toMutableSet() }.toMutableMap()
        novosActors.getOrPut(actor) { mutableSetOf() }.add(capability)
        return PolicyBroker(allowed + capability, novosActors)
    }
}
