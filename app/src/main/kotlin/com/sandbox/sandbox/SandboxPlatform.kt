package com.sandbox.sandbox

import com.brain.capability.CapabilityAvailability
import com.brain.capability.CapabilityCategory
import com.brain.capability.CapabilityDefinition
import com.brain.capability.CapabilityProvenance
import com.brain.capability.CapabilityRegistry
import com.brain.gateway.ActionGateway
import com.brain.gateway.InMemoryActionAuditLog
import com.brain.policy.PolicyBroker
import com.brain.execution.RiskClass
import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File

/** Fachada das fases locais; mantém o runtime como única porta de execução autorizada. */
class SandboxPlatform(
    val runtime: ManagedSandboxRuntime,
    workspaceRoot: File,
    componentStateFile: File,
    serviceStateDir: File,
    policy: SandboxSecurityPolicy = SandboxSecurityPolicy(),
    networkPolicy: NetworkPolicy = NetworkPolicy(),
    trustedRemotePluginSourceIds: Set<String> = emptySet()
) {
    private val gatewayCapabilities = CapabilityRegistry(
        listOf("sandbox.git", "sandbox.toolchain", "sandbox.test", "sandbox.diagnostics", "sandbox.plugin").map { id ->
            CapabilityDefinition(
                id = id,
                name = id,
                description = "capability Android Sandbox via ActionGateway",
                category = CapabilityCategory.SANDBOX,
                ownerId = "sandbox-platform",
                origin = "android-sandbox",
                risk = RiskClass.LOW,
                availability = CapabilityAvailability.AVAILABLE,
                provenance = listOf(CapabilityProvenance("android-sandbox", "sandbox-platform"))
            )
        }
    )
    private val policyBroker = PolicyBroker(
        allowedCapabilities = setOf("sandbox.git", "sandbox.toolchain", "sandbox.test", "sandbox.diagnostics", "sandbox.plugin"),
        actorCapabilities = mapOf("sandbox-platform" to setOf("sandbox.git", "sandbox.toolchain", "sandbox.test", "sandbox.diagnostics", "sandbox.plugin"))
    ).withCapabilityRegistry(gatewayCapabilities)
    private val gatewayExecutionLogs = GatewayBackedSandboxExecutor.logs()
    private val actionGateway = ActionGateway(
        registry = gatewayCapabilities,
        policy = policyBroker,
        executor = SandboxActionExecutor(ManagedRuntimeExecutor(runtime), gatewayExecutionLogs),
        audit = InMemoryActionAuditLog()
    )
    private val securedExecutor = SecureCommandExecutor(
        GatewayBackedSandboxExecutor(actionGateway, gatewayExecutionLogs), policy
    )
    private val remotePluginCatalog = RemotePluginCatalog(trustedRemotePluginSourceIds)
    private val componentJsonFile = File(componentStateFile.parentFile, "components.json")
    private val pluginSnapshotStore = PluginSnapshotStore(
        File(componentJsonFile.parentFile ?: componentJsonFile.absoluteFile.parentFile, "plugin_snapshots.json"),
        File(componentJsonFile.parentFile ?: componentJsonFile.absoluteFile.parentFile, "plugin_history.jsonl")
    )
    val plugins = SearchablePluginManager(
        securedExecutor,
        JsonComponentRepository(componentJsonFile, legacyTsvFile = componentStateFile),
        catalogProvider = { (BuiltInCatalog.all + remotePluginCatalog.components()).distinctBy { it.id } },
        snapshotStore = pluginSnapshotStore
    )
    val workspace = WorkspaceManager(workspaceRoot)
    val services = ServiceManager(securedExecutor, serviceStateDir, NetworkPolicyBroker(networkPolicy))
    val git = GitManager(securedExecutor)
    val diagnostics = SandboxDiagnostics(securedExecutor)
    val testLab = TestLab(securedExecutor)
    val toolchains = ToolchainManager(securedExecutor, File(workspaceRoot, "toolchains"))
    val security = SecurityAssessmentEngine()
    val securityScanner = SecurityProjectScanner()
    val securityScenarios = SecurityScenarioCatalog.baseline
    val securityPolicy: SandboxSecurityPolicy = policy
    /** Corpus de regressão persistente, executado somente com resultados sintéticos determinísticos. */
    val securityCorpus = SecurityRegressionCorpus(File(workspaceRoot, "security/security_regression_corpus.jsonl"))

    /** Executa scanner + probes determinísticos + assessment e registra os resultados observados. */
    fun runSecurityRegression(scanRoot: File): SecurityAssessment {
        val scan = securityScanner.scan(scanRoot)
        val results = securityCorpus.runDeterministic(securityScenarios)
        val assessment = security.evaluate(scan, securityScenarios, results)
        securityCorpus.record(assessment.lab, results)
        return assessment
    }

    fun securityCorpusDigest(): String = securityCorpus.digest()

    /** Importa um snapshot já coletado; não realiza rede, instalação ou execução. */
    fun importRemotePluginSnapshot(snapshot: RemoteCatalogSnapshot): RemoteCatalogResult = remotePluginCatalog.importSnapshot(snapshot)

    fun close() = runtime.shutdown()
}
