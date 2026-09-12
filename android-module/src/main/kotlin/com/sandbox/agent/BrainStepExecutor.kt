package com.sandbox.agent

import com.brain.execution.StepAttempt
import com.brain.execution.StepExecutor
import com.brain.planner.PassoPlano
import com.brain.router.ProviderModel

/** Ponte explícita: o coordenador decide; a sessão executa somente a capability autorizada. */
class BrainStepExecutor(private val session: AgentSandboxSession) : StepExecutor {
    override fun execute(step: PassoPlano, provider: ProviderModel?): StepAttempt {
        return when (val outcome = session.rodarCapacidade(step.parametros)) {
            is AgentSandboxSession.CommandOutcome.Completed -> StepAttempt(
                success = outcome.log.exitCode == 0,
                output = outcome.log.stdout,
                error = outcome.log.stderr.takeIf { it.isNotBlank() }
            )
            is AgentSandboxSession.CommandOutcome.Refused -> StepAttempt(false, error = outcome.reason)
        }
    }
}
