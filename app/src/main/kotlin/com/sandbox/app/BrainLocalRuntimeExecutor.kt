package com.sandbox.app

import android.content.Context
import com.sandbox.android.AndroidSandboxFactory
import com.sandbox.runtime.ManagedSandboxRuntime

/**
 * Executor temporário de homologação do Brain Local.
 * O Brain decide usar esta capacidade; este componente prepara/reusa o Sandbox
 * e devolve somente o texto da inferência para o Brain.
 */
internal class BrainLocalRuntimeExecutor(context: Context) {
    private val appContext = context.applicationContext
    private val factory = AndroidSandboxFactory(appContext)
    private val bridge = LocalLlmSandboxBridge(appContext)
    private var runtime: ManagedSandboxRuntime? = null

    fun complete(prompt: String): BrainApiGateway.LocalExecutionResult {
        require(prompt.isNotBlank()) { "prompt não pode ser vazio" }
        val manifest = LocalModelManifestLoader.load(appContext)
        val modelFile = java.io.File(appContext.filesDir, "sandbox/models/${manifest.id}.gguf")
        require(modelFile.isFile) { "Brain Local ainda não foi baixado." }
        val rootfs = java.io.File(appContext.filesDir, "sandbox/rootfs")
        require(rootfs.isDirectory) { "Sandbox ainda não está preparado." }

        val active = runtime ?: factory.prepareManagedRuntime(factory.persistentSessionId()).also { runtime = it }
        val engine = bridge.ensureEngine().getOrThrow()
        val model = bridge.linkModel(manifest.id)
            ?: error("Não foi possível disponibilizar o modelo local no Sandbox.")
        val command = bridge.buildCommand(engine, model, prompt, maxTokens = 200)
        val execution = active.execute(command, timeoutSeconds = 120, workingDir = "/home/sandbox")
        if (!execution.succeeded) {
            error(execution.stderr.ifBlank { execution.stdout }.ifBlank { "inferência local falhou" })
        }
        val text = execution.stdout.trim()
        require(text.isNotBlank()) { "Brain Local retornou resposta vazia." }
        return BrainApiGateway.LocalExecutionResult(
            text = text,
            modelId = "brain-local",
            source = com.brain.memory.KnowledgeSource(type = "local", uri = "brain://local")
        )
    }
}
