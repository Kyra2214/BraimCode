package com.brain.policy

/**
 * Opaque proof that a PolicyDecision was issued by PolicyBroker and has not
 * had its authorization-relevant fields changed through data-class copy().
 *
 * The constructor is private and the issuer is internal to the brain module;
 * consumers can only validate a token they already received from PolicyBroker.
 */
class AuthorizationToken private constructor(
    private val decisionId: String,
    private val runId: String,
    private val taskId: String,
    private val actor: String,
    private val capability: String,
    private val resource: String
) {
    fun matches(decision: PolicyDecision): Boolean =
        decisionId == decision.decisionId &&
            runId == decision.runId &&
            taskId == decision.taskId &&
            actor == decision.actor &&
            capability == decision.capability &&
            resource == decision.resource

    companion object {
        internal fun issue(decision: PolicyDecision): AuthorizationToken =
            AuthorizationToken(
                decisionId = decision.decisionId,
                runId = decision.runId,
                taskId = decision.taskId,
                actor = decision.actor,
                capability = decision.capability,
                resource = decision.resource
            )
    }
}
