package com.sandbox.runtime

/**
 * Resultado de um comando executado dentro do sandbox.
 *
 * [timedOut] é explícito em vez de inferido do exitCode, porque um comando
 * pode legitimamente sair com código != 0 por conta própria — misturar os
 * dois conceitos escondia esse tipo de causa em versões anteriores de
 * projetos parecidos.
 */
data class SandboxExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timedOut: Boolean
) {
    val succeeded: Boolean get() = !timedOut && exitCode == 0
}
