package com.sandbox.sandbox

import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File

/** Fachada das fases 1–8; mantém o runtime da Fase 0 como única porta de execução. */
class SandboxPlatform(
    val runtime: ManagedSandboxRuntime,
    workspaceRoot: File,
    componentStateFile: File,
    serviceStateDir: File,
    policy: SandboxSecurityPolicy = SandboxSecurityPolicy(),
    networkPolicy: NetworkPolicy = NetworkPolicy(),
    trustedRemotePluginSourceIds: Set<String> = emptySet()
) {
    private val securedExecutor = SecureCommandExecutor(ManagedRuntimeExecutor(runtime), policy)
    private val remotePluginCatalog = RemotePluginCatalog(trustedRemotePluginSourceIds)
    // Fase 1 Expandida: persistência em JSON (mais robusta que TSV) com
    // busca e filtros. `componentStateFile` (legado TSV) é migrado
    // automaticamente na primeira leitura, se existir.
    private val componentJsonFile = File(componentStateFile.parentFile, "components.json")
    val plugins = SearchablePluginManager(
        securedExecutor,
        JsonComponentRepository(componentJsonFile, legacyTsvFile = componentStateFile)
    ) { (BuiltInCatalog.all + remotePluginCatalog.components()).distinctBy { it.id } }
    val workspace = WorkspaceManager(workspaceRoot)
    val services = ServiceManager(securedExecutor, serviceStateDir, NetworkPolicyBroker(networkPolicy))
    val git = GitManager(securedExecutor)
    val diagnostics = SandboxDiagnostics(securedExecutor)
    val testLab = TestLab(securedExecutor)
    /** Todos os subsistemas usam o mesmo executor protegido e o mesmo workspace. */
    val toolchains = ToolchainManager(securedExecutor, File(workspaceRoot, "toolchains"))
    val security = SecurityAssessmentEngine()
    val securityScanner = SecurityProjectScanner()
    val securityScenarios = SecurityScenarioCatalog.baseline
    val securityPolicy: SandboxSecurityPolicy = policy

    /** Importa um snapshot já coletado; não realiza rede, instalação ou execução. */
    fun importRemotePluginSnapshot(snapshot: RemoteCatalogSnapshot): RemoteCatalogResult =
        remotePluginCatalog.importSnapshot(snapshot)

    fun close() = runtime.shutdown()
}
