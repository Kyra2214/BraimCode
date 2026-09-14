package com.sandbox.app

/**
 * Brain Local: adapter used by the Brain when no free external API is available.
 *
 * The UI never calls this class directly. The BrainApiGateway owns the decision
 * between external providers and this local capability.
 */
class LocalLlmBrainFallback(
    private val bridge: LocalLlmSandboxBridge
) {
    data class Result(
        val text: String,
        val modelId: String,
        val source: String = "brain-local"
    )

    fun complete(
        modelId: String,
        prompt: String,
        maxTokens: Int = 200
    ): Result {
        require(prompt.isNotBlank()) { "Prompt vazio." }

        val engine = bridge.ensureEngine().getOrThrow()
        val model = bridge.linkModel(modelId)
            ?: error("Modelo local não encontrado: $modelId")
        val command = bridge.buildCommand(engine, model, prompt, maxTokens)

        // The Sandbox executor remains the only component allowed to execute
        // this capability. This adapter only produces the typed execution request.
        error(
            "Brain Local preparado para execução no Sandbox. " +
                "Nenhum executor local deve ser chamado diretamente pelo chat. " +
                "Comando: ${command.first()}"
        )
    }
}
