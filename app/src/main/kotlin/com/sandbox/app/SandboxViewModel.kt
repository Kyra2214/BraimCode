package com.sandbox.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sandbox.android.AndroidSandboxFactory
import com.sandbox.agent.BrainSandboxController
import com.sandbox.agent.ResultadoCiclo
import com.sandbox.resource.SandboxResourceManager
import com.sandbox.runtime.ExecutionLog
import com.sandbox.runtime.ManagedSandboxRuntime
import com.sandbox.runtime.SandboxExecutionResult
import com.sandbox.runtime.SandboxState
import com.sandbox.sandbox.BuiltInCatalog
import com.sandbox.sandbox.ComponentKind
import com.sandbox.sandbox.InstallationState
import com.sandbox.sandbox.InstalledComponent
import com.sandbox.sandbox.SandboxComponent
import com.sandbox.sandbox.SandboxPlatform
import com.sandbox.sandbox.SecurityAssessment
import com.sandbox.sandbox.PluginOperationRecord
import com.sandbox.sandbox.PluginSnapshot
import com.sandbox.sandbox.ToolchainStatus
import com.brain.planner.PlanoExecucao
import com.sandbox.sandbox.Project
import com.sandbox.sandbox.ServiceStatus
import com.sandbox.sandbox.BuiltInServices
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SandboxPhase {
    data object NotReady : SandboxPhase
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : SandboxPhase
    data class Preparing(
        val stage: String = "Preparando runtime",
        val bytesCompleted: Long = 0L,
        val totalBytes: Long = 0L
    ) : SandboxPhase
    data object Ready : SandboxPhase
    data object Running : SandboxPhase
    data class Blocked(val reason: String) : SandboxPhase
}

data class QuickCommand(val label: String, val command: String)

val QUICK_COMMANDS: List<QuickCommand> = listOf(
    QuickCommand("git --version", "git --version"),
    QuickCommand("git clone", "git clone --depth 1 https://github.com/octocat/Hello-World.git /tmp/hello && ls /tmp/hello"),
    QuickCommand("pip --version", "python3 -m pip --version"),
    QuickCommand("pip install", "python3 -m pip install --quiet requests && python3 -c \"import requests; print(requests.__version__)\""),
    QuickCommand("npm -v", "npm -v"),
    QuickCommand("npm install", "cd /tmp && npm init -y >/dev/null 2>&1 && npm install --no-fund --no-audit lodash && node -e \"console.log(require('lodash').VERSION)\""),
    QuickCommand("node -v", "node -v"),
    QuickCommand("zip/unzip", "cd /tmp && echo conteudo > arquivo.txt && zip -q teste.zip arquivo.txt && unzip -l teste.zip"),
    QuickCommand("curl", "curl -sI https://example.com | head -n 1"),
    QuickCommand("sqlite3", "sqlite3 --version"),
    QuickCommand("gcc/make", "gcc --version | head -n 1 && make --version | head -n 1"),
    QuickCommand("apt list", "apt list --installed 2>/dev/null | wc -l")
)

class SandboxViewModel(application: Application) : AndroidViewModel(application) {
    private val factory = AndroidSandboxFactory(application)
    private var runtime: ManagedSandboxRuntime? = null
    private var platform: SandboxPlatform? = null
    private var brainController: BrainSandboxController? = null
    private var brainIntegration: BrainIntegrationFacade? = null

    var phase by mutableStateOf<SandboxPhase>(SandboxPhase.NotReady)
        private set

    var localModelProgress by mutableStateOf<Pair<Long, Long>?>(null)
        private set
    var localModelReady by mutableStateOf(false)
        private set
    var localModelError by mutableStateOf<String?>(null)
        private set

    // --- Plugins / Ferramentas (Expansão Fase 1) ---

    /** IDs com instalação/remoção em andamento — usado para status visual em tempo real. */
    var installingComponentIds by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Incrementado após toda instalação/remoção para forçar recomposição das listas. */
    var pluginListVersion by mutableStateOf(0)
        private set

    /**
     * Cache em memória do status de cada componente, atualizado em background (IO).
     * A tela de Plugins lê só daqui — nunca do repositório diretamente — pra não fazer
     * leitura de disco na thread principal a cada recomposição (isso causava o travamento
     * ao clicar em "Instalar").
     */
    private var statusCache by mutableStateOf<Map<String, InstalledComponent>>(emptyMap())

    var lastPluginError by mutableStateOf<String?>(null)
        private set
    var pluginSnapshots by mutableStateOf<List<PluginSnapshot>>(emptyList())
        private set
    var pluginHistory by mutableStateOf<List<PluginOperationRecord>>(emptyList())
        private set

    val sandboxReadyForPlugins: Boolean get() = platform != null
    var commandInput by mutableStateOf("echo hello from sandbox")
    var lastResult by mutableStateOf<SandboxExecutionResult?>(null)
        private set
    var lastExecution by mutableStateOf<ExecutionLog?>(null)
        private set
    var diagnosticsReport by mutableStateOf<String?>(null)
        private set
    var lastBrainCycle by mutableStateOf<ResultadoCiclo?>(null)
        private set
    var lastTestLabReport by mutableStateOf<com.sandbox.sandbox.TestLabReport?>(null)
        private set
    var lastSecurityAssessment by mutableStateOf<SecurityAssessment?>(null)
        private set
    var toolchainStatuses by mutableStateOf<Map<String, ToolchainStatus>>(emptyMap())
        private set
    var pendingApprovalId by mutableStateOf<String?>(null)
        private set
    private var pendingApprovalPlan: PlanoExecucao? = null
    private var pendingApprovalRunId: String? = null
    var workspaceProjectName by mutableStateOf("demo-project")
    var workspaceProjects by mutableStateOf<List<Project>>(emptyList())
        private set
    var lastGitStatus by mutableStateOf<String?>(null)
        private set
    var sqliteServiceStatus by mutableStateOf<ServiceStatus?>(null)
        private set
    var workspaceError by mutableStateOf<String?>(null)
        private set
    var brainSkillSummary by mutableStateOf<List<String>>(emptyList())
        private set
    var lastWorkflowStatus by mutableStateOf<String?>(null)
        private set
    var memorySuccessRate by mutableStateOf<Double?>(null)
        private set
    var discoverySummary by mutableStateOf<String?>(null)
        private set
    var deliverySummary by mutableStateOf<String?>(null)
        private set

    fun runDiagnostics() {
        viewModelScope.launch {
            diagnosticsReport = withContext(Dispatchers.IO) {
                runCatching { factory.inspectExtractedRootfs() }
                    .getOrElse { "Falha ao inspecionar rootfs: ${it.message}" }
            }
        }
    }

    fun prepareSandbox() {
        if (phase is SandboxPhase.Downloading || phase is SandboxPhase.Preparing) return
        viewModelScope.launch {
            phase = SandboxPhase.Downloading(0, 0)
            val manifests = try {
                ManifestLoader.loadAll(getApplication())
            } catch (e: IllegalStateException) {
                phase = SandboxPhase.Blocked(e.message ?: "Manifesto inválido")
                return@launch
            }
            val totalBytes = manifests.sumOf { it.sizeBytes }
            var completedBytes = 0L
            val downloadResult = withContext(Dispatchers.IO) {
                manifests.mapIndexed { index, manifest ->
                    val result = factory.layerResourceManager(index).ensureAvailable(manifest) { downloaded, _ ->
                        phase = SandboxPhase.Downloading(completedBytes + downloaded, totalBytes)
                    }
                    if (result is SandboxResourceManager.DownloadResult.Success) {
                        completedBytes += manifest.sizeBytes
                    }
                    result
                }
            }
            val failedDownload = downloadResult.filterIsInstance<SandboxResourceManager.DownloadResult.Failure>().firstOrNull()
            if (failedDownload != null) {
                    phase = SandboxPhase.Blocked(failedDownload.reason)
                    return@launch
            }
            phase = SandboxPhase.Preparing("Extraindo RootFS", 0L, manifests.sumOf { it.sizeBytes })
            try {
                runtime?.shutdown()
                val preparedRuntime = withContext(Dispatchers.IO) {
                    factory.prepareManagedRuntime(factory.persistentSessionId()) { completed, total, stage ->
                        phase = SandboxPhase.Preparing(stage, completed, total)
                    }
                }
                runtime = preparedRuntime
                val sandboxDir = File(getApplication<Application>().filesDir, "sandbox")
                brainController = BrainSandboxController(
                    runtime = preparedRuntime,
                    rootfsDir = File(sandboxDir, "rootfs")
                )
                brainIntegration = BrainIntegrationFacade(File(sandboxDir, "brain"))
                platform = SandboxPlatform(
                    runtime = preparedRuntime,
                    workspaceRoot = File(sandboxDir, "workspace"),
                    componentStateFile = File(sandboxDir, "components.tsv"),
                    serviceStateDir = File(sandboxDir, "services")
                )
                pluginListVersion++
                refreshStatusCache()
                refreshPluginAudit()
                phase = SandboxPhase.Ready
            } catch (e: Exception) {
                runtime = null
                phase = SandboxPhase.Blocked(e.message ?: "Falha ao preparar o runtime")
            }
        }
    }

    fun runCommand() {
        val activeRuntime = runtime ?: return
        if (phase != SandboxPhase.Ready) return
        val command = commandInput.trim()
        if (command.isEmpty()) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            val execution = withContext(Dispatchers.IO) {
                runCatching {
                    activeRuntime.execute(
                        command = listOf("/bin/bash", "-c", command),
                        timeoutSeconds = 60,
                        workingDir = "/home/sandbox"
                    )
                }.getOrElse { _ ->
                    null
                }
            }
            if (execution == null) {
                phase = SandboxPhase.Ready
                return@launch
            }
            lastExecution = execution
            lastResult = execution.toUiResult()
            phase = SandboxPhase.Ready
        }
    }

    fun downloadLocalModel() {
        if (localModelProgress != null || localModelReady) return
        viewModelScope.launch {
            localModelError = null
            val manifest = runCatching { LocalModelManifestLoader.load(getApplication()) }
                .getOrElse {
                    localModelError = it.message ?: "Manifesto da mini-LLM inválido"
                    return@launch
                }
            val result = withContext(Dispatchers.IO) {
                factory.modelResourceManager(manifest.id).ensureAvailable(manifest) { downloaded, total ->
                    localModelProgress = downloaded to total
                }
            }
            localModelProgress = null
            when (result) {
                is SandboxResourceManager.DownloadResult.Success -> localModelReady = true
                is SandboxResourceManager.DownloadResult.Failure -> localModelError = result.reason
            }
        }
    }

    fun cancelCommand() {
        viewModelScope.launch(Dispatchers.IO) {
            runtime?.cancel()
        }
    }

    /** Primeiro caminho acionado pela UI que passa pelo Brain e não pelo executor direto. */
    fun runBrainHealthCheck() {
        val controller = brainController
        if (controller == null || phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            val cycle = withContext(Dispatchers.IO) {
                runCatching { controller.healthCheck(factory.persistentSessionId()) }.getOrNull()
            }
            lastBrainCycle = cycle
            phase = SandboxPhase.Ready
        }
    }

    /** Executa o TestLab pelo executor protegido do SandboxPlatform. */
    fun runTestLab(projectPath: String = "/home/sandbox/workspace") {
        val plat = platform ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            lastTestLabReport = withContext(Dispatchers.IO) { runCatching { plat.testLab.run(projectPath) }.getOrNull() }
            phase = SandboxPhase.Ready
        }
    }

    /** Executa o scanner e o gate de segurança catalogado no workspace atual. */
    fun runSecurityAssessment() {
        val plat = platform ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            lastSecurityAssessment = withContext(Dispatchers.IO) {
                runCatching {
                    val root = File(getApplication<Application>().filesDir, "sandbox/workspace")
                    val scan = plat.securityScanner.scan(root)
                    plat.security.evaluate(scan, plat.securityScenarios, emptyList())
                }.getOrNull()
            }
            phase = SandboxPhase.Ready
        }
    }

    fun refreshToolchains() {
        val plat = platform ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val statuses = com.sandbox.sandbox.BuiltInToolchains.all.associate { it.id to plat.toolchains.status(it.id) }
            withContext(Dispatchers.Main) { toolchainStatuses = statuses }
        }
    }

    fun installToolchain(id: String) {
        val plat = platform ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            val status = withContext(Dispatchers.IO) { runCatching { plat.toolchains.install(id) }.getOrNull() }
            if (status != null) toolchainStatuses = toolchainStatuses + (id to status)
            phase = SandboxPhase.Ready
        }
    }

    fun requestApprovalDemo() {
        val controller = brainController ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            val plan = controller.approvalDemoPlan()
            val runId = "approval-${System.currentTimeMillis()}"
            val cycle = withContext(Dispatchers.IO) { controller.executePlan(plan, runId) }
            pendingApprovalPlan = plan
            pendingApprovalRunId = runId
            pendingApprovalId = cycle.passos.firstOrNull()?.approvalId
            lastBrainCycle = cycle
            phase = SandboxPhase.Ready
        }
    }

    fun approveAndResume() {
        val controller = brainController ?: return
        val plan = pendingApprovalPlan ?: return
        val runId = pendingApprovalRunId ?: return
        val approvalId = pendingApprovalId ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            phase = SandboxPhase.Running
            val cycle = withContext(Dispatchers.IO) { controller.resumePlan(plan, runId, approvalId) }
            lastBrainCycle = cycle
            pendingApprovalId = null
            pendingApprovalPlan = null
            pendingApprovalRunId = null
            phase = SandboxPhase.Ready
        }
    }

    fun refreshWorkspace() {
        val plat = platform ?: return
        workspaceProjects = plat.workspace.listProjects()
        sqliteServiceStatus = plat.services.status(BuiltInServices.sqlite("/home/sandbox/workspace"))
    }

    fun createWorkspaceProject() {
        val plat = platform ?: return
        workspaceError = null
        runCatching { plat.workspace.createProject(workspaceProjectName) }
            .onSuccess { refreshWorkspace() }
            .onFailure { workspaceError = it.message ?: "Falha ao criar projeto" }
    }

    fun inspectGitStatus() {
        val plat = platform ?: return
        val project = workspaceProjects.firstOrNull { it.name == workspaceProjectName }
        if (project == null) {
            workspaceError = "Crie ou selecione um projeto antes de consultar o Git."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val status = plat.git.status("/home/sandbox/workspace/projects/${project.name}")
            withContext(Dispatchers.Main) { lastGitStatus = status.stdout.ifBlank { status.stderr } }
        }
    }

    fun startSqliteService() {
        val plat = platform ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val status = runCatching { plat.services.start(BuiltInServices.sqlite("/home/sandbox/workspace")) }.getOrNull()
            withContext(Dispatchers.Main) { sqliteServiceStatus = status }
        }
    }

    fun stopSqliteService() {
        val plat = platform ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val status = plat.services.stop(BuiltInServices.sqlite("/home/sandbox/workspace"))
            withContext(Dispatchers.Main) { sqliteServiceStatus = status }
        }
    }

    fun refreshBrainCatalogs() {
        val integration = brainIntegration ?: return
        brainSkillSummary = integration.enabledSkills().map { "${it.manifest.id} (${it.manifest.capabilities.joinToString()})" }
        viewModelScope.launch(Dispatchers.IO) {
            val rate = integration.memoryRate()
            withContext(Dispatchers.Main) { memorySuccessRate = rate }
        }
    }

    fun runBrainWorkflow() {
        val integration = brainIntegration ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { integration.runHealthWorkflow("workflow-${System.currentTimeMillis()}") }.getOrNull()
            result?.let { integration.recordExperience(it.runId, it.status.name == "COMPLETED") }
            withContext(Dispatchers.Main) { lastWorkflowStatus = result?.status?.name ?: "FAILED" }
        }
    }

    fun runDiscovery() {
        val integration = brainIntegration ?: return
        discoverySummary = runCatching {
            val result = integration.discoverBuiltInCandidate()
            "${result.radar.accepted} candidato(s) aceito(s), ${result.radar.rejected} rejeitado(s), ${result.workspace.windows.size} janela(s)"
        }.getOrElse { "Discovery falhou: ${it.message}" }
    }

    /** Gera um recibo local com hashes dos artefatos do workspace, sem rede. */
    fun publishLocalDelivery() {
        val integration = brainIntegration ?: return
        if (phase != SandboxPhase.Ready) return
        viewModelScope.launch(Dispatchers.IO) {
            val root = File(getApplication<Application>().filesDir, "sandbox/workspace")
            val summary = runCatching {
                val receipt = integration.publishLocalDelivery(root, "delivery-${System.currentTimeMillis()}")
                "Recibo local: ${receipt.artifacts.size} artefato(s), " +
                    "${receipt.artifacts.sumOf { it.bytes }} bytes, " +
                    "${receipt.artifacts.firstOrNull()?.sha256?.take(12) ?: "sem arquivos"}"
            }.getOrElse { "Falha na entrega local: ${it.message ?: "erro desconhecido"}" }
            withContext(Dispatchers.Main) { deliverySummary = summary }
        }
    }

    fun resetSandbox() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runtime?.reset { factory.purgeAll() }
                runtime = null
                brainController = null
                brainIntegration = null
                factory.clearPersistentSession()
            }
            platform = null
            installingComponentIds = emptySet()
            statusCache = emptyMap()
            pluginSnapshots = emptyList()
            pluginHistory = emptyList()
            pluginListVersion++
            lastResult = null
            lastExecution = null
            diagnosticsReport = null
            lastBrainCycle = null
            lastTestLabReport = null
            lastSecurityAssessment = null
            toolchainStatuses = emptyMap()
            pendingApprovalId = null
            pendingApprovalPlan = null
            pendingApprovalRunId = null
            workspaceProjects = emptyList()
            lastGitStatus = null
            sqliteServiceStatus = null
            workspaceError = null
            brainSkillSummary = emptyList()
            lastWorkflowStatus = null
            memorySuccessRate = null
            discoverySummary = null
            deliverySummary = null
            localModelProgress = null
            localModelReady = false
            localModelError = null
            phase = SandboxPhase.NotReady
        }
    }

    fun recentExecutions(limit: Int = 20): List<ExecutionLog> =
        runtime?.getRecentExecutions(limit) ?: emptyList()

    /**
     * Componentes do catálogo, já filtrados por tipo/busca/instalado.
     * Funciona mesmo antes do sandbox estar pronto (navegação do catálogo
     * é sempre permitida; instalar exige o sandbox em SandboxPhase.Ready).
     */
    fun pluginComponents(kind: ComponentKind, query: String, installedOnly: Boolean): List<SandboxComponent> {
        // Filtra só em memória (catálogo + statusCache) — nunca toca o disco aqui,
        // pra não travar a thread principal a cada recomposição da lista.
        val base = (platform?.plugins?.components() ?: BuiltInCatalog.all)
            .filter { it.kind == kind }
        val searched = if (query.isBlank()) base else base.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
        if (!installedOnly) return searched
        return searched.filter { statusCache[it.id]?.state == InstallationState.INSTALLED }
    }

    fun pluginStatus(id: String): InstalledComponent? = statusCache[id]

    /** Recarrega o cache de status em background (IO) e publica o resultado na main thread. */
    private fun refreshStatusCache() {
        val plat = platform ?: run { statusCache = emptyMap(); return }
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = plat.plugins.components().mapNotNull { component ->
                plat.plugins.status(component.id)?.let { component.id to it }
            }.toMap()
            withContext(Dispatchers.Main) { statusCache = snapshot }
        }
    }

    fun installComponent(id: String) {
        val plat = platform
        if (plat == null) {
            lastPluginError = "Prepare o sandbox antes de instalar componentes."
            return
        }
        if (id in installingComponentIds) return
        lastPluginError = null
        installingComponentIds = installingComponentIds + id
        viewModelScope.launch {
            var result: Result<InstalledComponent>? = null
            try {
                result = withContext(Dispatchers.IO) { runCatching { plat.plugins.install(id) } }
                result.onFailure { lastPluginError = it.message ?: "Falha ao instalar $id" }
                result.getOrNull()?.let {
                    if (it.state == InstallationState.FAILED) {
                        lastPluginError = it.error ?: "Falha ao instalar $id"
                    }
                }
            } finally {
                installingComponentIds = installingComponentIds - id
                result?.getOrNull()?.let { updated -> statusCache = statusCache + (id to updated) }
                refreshPluginAudit()
                pluginListVersion++
            }
        }
    }

    fun removeComponent(id: String) {
        val plat = platform
        if (plat == null) {
            lastPluginError = "Prepare o sandbox antes de remover componentes."
            return
        }
        if (id in installingComponentIds) return
        lastPluginError = null
        installingComponentIds = installingComponentIds + id
        viewModelScope.launch {
            var result: Result<InstalledComponent?>? = null
            try {
                result = withContext(Dispatchers.IO) { runCatching { plat.plugins.remove(id) } }
                result.onFailure { lastPluginError = it.message ?: "Falha ao remover $id" }
                result.getOrNull()?.let {
                    if (it.state == InstallationState.FAILED) {
                        lastPluginError = it.error ?: "Falha ao remover $id"
                    }
                }
            } finally {
                installingComponentIds = installingComponentIds - id
                result?.let { r ->
                    val updated = r.getOrNull()
                    statusCache = if (updated != null) statusCache + (id to updated) else statusCache - id
                }
                refreshPluginAudit()
                pluginListVersion++
            }
        }
    }

    fun clearPluginError() {
        lastPluginError = null
    }

    fun refreshPluginAudit() {
        val plat = platform ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val snapshots = plat.plugins.snapshots()
            val history = plat.plugins.history()
            withContext(Dispatchers.Main) {
                pluginSnapshots = snapshots
                pluginHistory = history
            }
        }
    }

    fun rollbackPlugins(version: Long) {
        val plat = platform ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { plat.plugins.rollback(version) } }
                .onFailure { lastPluginError = it.message ?: "Falha ao restaurar snapshot v$version" }
            refreshStatusCache()
            refreshPluginAudit()
            pluginListVersion++
        }
    }

    private fun ExecutionLog.toUiResult(): SandboxExecutionResult = SandboxExecutionResult(
        stdout = stdout,
        stderr = stderr,
        exitCode = exitCode ?: -1,
        timedOut = timedOut
    )
}
