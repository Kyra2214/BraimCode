package com.sandbox.sandbox

import com.brain.execution.RiskClass
import com.brain.policy.PolicyBroker
import com.brain.policy.PolicyContext
import com.sandbox.runtime.ExecutionLog

/** Ponte obrigatória para Git, toolchains, TestLab, diagnostics e plugins. */
class PolicyGatedExecutor(
    private val delegate: SandboxCommandExecutor,
    private val policy: PolicyBroker,
    private val actor: String = "sandbox-platform"
) : SandboxCommandExecutor {
    override fun execute(command: List<String>, timeoutSeconds: Long, workingDir: String): ExecutionLog {
        require(command.isNotEmpty()) { "comando vazio" }
        val capability = when (command.first().substringAfterLast('/')) {
            "git" -> "sandbox.git"
            "ps", "uname", "id" -> "sandbox.diagnostics"
            "python", "python3", "node", "gcc", "g++", "javac", "go" -> "sandbox.toolchain"
            "sandbox-build", "sandbox-test" -> "sandbox.test"
            else -> "sandbox.plugin"
        }
        val context = PolicyContext(
            runId = "platform-execution",
            taskId = capability,
            actor = actor,
            riskClass = RiskClass.LOW,
            sandboxRequired = true,
            networkAllowed = false,
            filesystemRoots = listOf(workingDir)
        )
        val decision = policy.authorize(actor, capability, workingDir, context)
        check(policy.check(decision, workingDir)) { "execução negada para $capability: ${decision.reason}" }
        return delegate.execute(command, timeoutSeconds, workingDir)
    }
}
