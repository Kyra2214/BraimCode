package com.sandbox.sandbox

import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File

/** Fachada das fases 1–8; mantém o runtime da Fase 0 como única porta de execução. */
class SandboxPlatform(
    val runtime: ManagedSandboxRuntime,
    workspaceRoot: File,
    componentStateFile: File,
    serviceStateDir: File,
    policy: SandboxSecurityPolicy = SandboxSecurityPolicy()
) {
    private val securedExecutor = SecureCommandExecutor(ManagedRuntimeExecutor(runtime), policy)
    // Fase 1 Expandida: persistência em JSON (mais robusta que TSV) com
    // busca e filtros. `componentStateFile` (legado TSV) é migrado
    // automaticamente na primeira leitura, se existir.
    private val componentJsonFile = File(componentStateFile.parentFile, "components.json")
    val plugins = SearchablePluginManager(
        securedExecutor,
        JsonComponentRepository(componentJsonFile, legacyTsvFile = componentStateFile)
    )
    val workspace = WorkspaceManager(workspaceRoot)
    val services = ServiceManager(securedExecutor, serviceStateDir)
    val git = GitManager(securedExecutor)
    val diagnostics = SandboxDiagnostics(securedExecutor)
    val testLab = TestLab(securedExecutor)
    val securityPolicy: SandboxSecurityPolicy = policy
    fun close() = runtime.shutdown()
}
