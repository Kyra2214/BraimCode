package com.sandbox.agent

import com.brain.planner.PlanoExecucao

/**
 * Fase 2: ponto único de entrada para a execução Android.
 * O ciclo aplica Router, Policy, Sandbox e validação na mesma jornada; esta
 * ponte evita que a UI escolha um executor paralelo e preserve o contrato de
 * que toda sessão nasce de uma autorização emitida pela Policy.
 */
class BrainSandboxExecutionBridge(private val ciclo: CicloExecucaoPlano) {
    fun execute(plano: PlanoExecucao, runId: String, actor: String): ResultadoCiclo =
        ciclo.executar(plano, runId, actor)
}
