package com.sandbox.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.sandbox.sandbox.SelfCheckReport
import com.sandbox.sandbox.SelfCheckSection
import com.sandbox.sandbox.SelfCheckItem
import com.sandbox.sandbox.SelfCheckStatus
import com.brain.planner.PlanoExecucao
import com.sandbox.sandbox.Project
import com.sandbox.sandbox.ServiceStatus
import com.sandbox.sandbox.BuiltInServices
import java.io.File
import com.sandbox.runtime.NamespaceSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SandboxPhase {
    data object NotReady : SandboxPhase
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : SandboxPhase
    data class Preparing(val stage: String = "Preparando runtime", val bytesCompleted: Long = 0L, val totalBytes: Long = 0L) : SandboxPhase
    data object Ready : SandboxPhase
    data object Running : SandboxPhase
    data class Blocked(val reason: String) : SandboxPhase
}

data class QuickCommand(val label: String, val command: String)

enum class ChatRole { USER, ASSISTANT, ERROR }
data class ChatMessage(val role: ChatRole, val content: String)

sealed interface ApiKeyTestUiState {
    data object Idle : ApiKeyTestUiState
    data object Testing : ApiKeyTestUiState
    data class Success(val message: String) : ApiKeyTestUiState
    data class Failure(val message: String) : ApiKeyTestUiState
}

data class CliToolCheck(val label: String, val script: String)
private val CLI_TOOL_CHECKS = listOf(
    CliToolCheck("git", "git --version"), CliToolCheck("curl", "curl --version | head -n 1"),
    CliToolCheck("sqlite3", "sqlite3 --version"), CliToolCheck("make", "make --version | head -n 1"),
    CliToolCheck("zip/unzip", "zip -v | head -n 1 && unzip -v | head -n 1"),
    CliToolCheck("pip (python3 -m pip)", "python3 -m pip --version"), CliToolCheck("npm", "npm -v")
)

val QUICK_COMMANDS = listOf(
    QuickCommand("git --version", "git --version"),
    QuickCommand("git clone", "git clone --depth 1 https://github.com/octocat/Hello-World.git /tmp/hello && ls /tmp/hello"),
    QuickCommand("pip --version", "python3 -m pip --version"),
    QuickCommand("pip install", "python3 -m pip install --quiet requests && python3 -c \"import requests; print(requests.__version__)\""),
    QuickCommand("npm -v", "npm -v"),
    QuickCommand("npm install", "cd /tmp && npm init -y >/dev/null 2>&1 && npm install --no-fund --no-audit lodash && node -e \"console.log(require('lodash').VERSION)\""),
    QuickCommand("node -v", "node -v"), QuickCommand("zip/unzip", "cd /tmp && echo conteudo > arquivo.txt && zip -q teste.zip arquivo.txt && unzip -l teste.zip"),
    QuickCommand("curl", "curl -sI https://example.com | head -n 1"), QuickCommand("sqlite3", "sqlite3 --version"),
    QuickCommand("gcc/make", "gcc --version | head -n 1 && make --version | head -n 1"), QuickCommand("apt list", "apt list --installed 2>/dev/null | wc -l")
)

class SandboxViewModel(application: Application) : AndroidViewModel(application) {
    private val factory = AndroidSandboxFactory(application)
    val namespaceSupport = NamespaceSupport.detect()
    private var runtime: ManagedSandboxRuntime? = null
    private var platform: SandboxPlatform? = null
    private var brainController: BrainSandboxController? = null
    private var brainIntegration: BrainIntegrationFacade? = null

    var phase by mutableStateOf<SandboxPhase>(SandboxPhase.NotReady); private set
    var localModelProgress by mutableStateOf<Pair<Long, Long>?>(null); private set
    var localModelReady by mutableStateOf(false); private set
    var localModelError by mutableStateOf<String?>(null)
    val chatMessages = mutableStateListOf<ChatMessage>()
    var chatInput by mutableStateOf("")
    var chatRunning by mutableStateOf(false); private set

    private val apiKeyStore = ApiKeyStore(application)
    val apiProviders: List<ApiProvider> = runCatching { ApiKeyCatalogLoader.load(application) }.getOrElse { emptyList() }

    /** UI -> Brain. O chat não chama runtime, llama.cpp, provider ou HTTP diretamente. */
    private val brainApiGateway = BrainApiGateway(apiProviders, apiKeyStore)

    private val apiKeyInputs = mutableStateMapOf<String, String>()
    val apiKeyTestState = mutableStateMapOf<String, ApiKeyTestUiState>()

    var installingComponentIds by mutableStateOf<Set<String>>(emptySet()); private set
    var pluginListVersion by mutableStateOf(0); private set
    private var statusCache by mutableStateOf<Map<String, InstalledComponent>>(emptyMap())
    var lastPluginError by mutableStateOf<String?>(null); private set
    var pluginSnapshots by mutableStateOf<List<PluginSnapshot>>(emptyList()); private set
    var pluginHistory by mutableStateOf<List<PluginOperationRecord>>(emptyList()); private set
    val sandboxReadyForPlugins get() = platform != null
    var commandInput by mutableStateOf("echo hello from sandbox")
    var lastResult by mutableStateOf<SandboxExecutionResult?>(null); private set
    var lastExecution by mutableStateOf<ExecutionLog?>(null); private set
    fun clearTerminal() { commandInput = ""; lastResult = null; lastExecution = null }
    var diagnosticsReport by mutableStateOf<String?>(null); private set
    var lastBrainCycle by mutableStateOf<ResultadoCiclo?>(null); private set
    var lastTestLabReport by mutableStateOf<com.sandbox.sandbox.TestLabReport?>(null); private set
    var lastSecurityAssessment by mutableStateOf<SecurityAssessment?>(null); private set
    var toolchainStatuses by mutableStateOf<Map<String, ToolchainStatus>>(emptyMap()); private set
    var pendingApprovalId by mutableStateOf<String?>(null); private set
    private var pendingApprovalPlan: PlanoExecucao? = null
    private var pendingApprovalRunId: String? = null
    var workspaceProjectName by mutableStateOf("demo-project")
    var workspaceProjects by mutableStateOf<List<Project>>(emptyList()); private set
    var lastGitStatus by mutableStateOf<String?>(null); private set
    var sqliteServiceStatus by mutableStateOf<ServiceStatus?>(null); private set
    var workspaceError by mutableStateOf<String?>(null); private set
    var brainSkillSummary by mutableStateOf<List<String>>(emptyList()); private set
    var lastWorkflowStatus by mutableStateOf<String?>(null); private set
    var memorySuccessRate by mutableStateOf<Double?>(null); private set
    var discoverySummary by mutableStateOf<String?>(null); private set
    var deliverySummary by mutableStateOf<String?>(null); private set
    var selfCheckReport by mutableStateOf<SelfCheckReport?>(null); private set
    var selfCheckRunning by mutableStateOf(false); private set
    var selfCheckStage by mutableStateOf<String?>(null); private set

    fun runFullSelfCheck() {
        val plat = platform ?: return
        if (selfCheckRunning || phase != SandboxPhase.Ready) return
        viewModelScope.launch {
            selfCheckRunning = true; phase = SandboxPhase.Running
            try {
                selfCheckStage = "Verificando toolchains (Java, Python, Node, C/C++, Rust, Go, Android SDK)..."
                val toolchainItems = withContext(Dispatchers.IO) { com.sandbox.sandbox.BuiltInToolchains.all.map { profile ->
                    val status = runCatching { plat.toolchains.refreshStatus(profile.id) }.getOrElse { e -> ToolchainStatus(profile.id, com.sandbox.sandbox.ToolchainState.FAILED, error = e.message ?: e.javaClass.simpleName) }
                    withContext(Dispatchers.Main) { toolchainStatuses = toolchainStatuses + (profile.id to status) }
                    val optIn = profile.id == "android"
                    val itemStatus = when (status.state) { com.sandbox.sandbox.ToolchainState.INSTALLED -> SelfCheckStatus.OK; com.sandbox.sandbox.ToolchainState.NOT_INSTALLED -> if (optIn) SelfCheckStatus.WARNING else SelfCheckStatus.FAILED; else -> SelfCheckStatus.FAILED }
                    val detail = when { status.state == com.sandbox.sandbox.ToolchainState.INSTALLED -> status.versionOutput.lineSequence().firstOrNull()?.take(120) ?: "instalado"; optIn && status.state == com.sandbox.sandbox.ToolchainState.NOT_INSTALLED -> "opcional — instale na aba Operações se precisar"; else -> (status.error ?: "não encontrado").take(200) }
                    SelfCheckItem(profile.displayName, itemStatus, detail)
                } }
                selfCheckStage = "Verificando ferramentas de linha de comando..."
                val cliItems = withContext(Dispatchers.IO) { CLI_TOOL_CHECKS.map { check ->
                    val active = runtime
                    if (active == null) SelfCheckItem(check.label, SelfCheckStatus.FAILED, "runtime indisponível") else {
                        val e = runCatching { active.execute(listOf("bash", "-c", check.script), timeoutSeconds = 15, workingDir = "/home/sandbox") }.getOrNull()
                        when { e == null -> SelfCheckItem(check.label, SelfCheckStatus.FAILED, "falha ao executar a checagem"); e.succeeded -> SelfCheckItem(check.label, SelfCheckStatus.OK, e.stdout.lineSequence().firstOrNull { it.isNotBlank() }?.take(120) ?: "instalado"); else -> SelfCheckItem(check.label, SelfCheckStatus.FAILED, e.stderr.ifBlank { e.stdout }.ifBlank { "comando não encontrado" }.take(160)) }
                    }
                } }
                selfCheckStage = "Verificando plugins e ferramentas opcionais instalados..."
                val pluginItems = withContext(Dispatchers.IO) { plat.plugins.components().mapNotNull { c ->
                    val cached = plat.plugins.status(c.id); if (cached == null || cached.state != InstallationState.INSTALLED) return@mapNotNull null
                    val works = runCatching { plat.plugins.validate(c) }.getOrDefault(false)
                    SelfCheckItem(c.name, if (works) SelfCheckStatus.OK else SelfCheckStatus.FAILED, if (works) (cached.version ?: "instalado") else "validação falhou")
                } }
                val llmItem = when { localModelReady -> SelfCheckItem("SmolLM2 135M Instruct (mini-LLM local)", SelfCheckStatus.OK, "baixada e verificada por SHA-256"); localModelError != null -> SelfCheckItem("SmolLM2 135M Instruct (mini-LLM local)", SelfCheckStatus.WARNING, "download com falha: $localModelError"); else -> SelfCheckItem("SmolLM2 135M Instruct (mini-LLM local)", SelfCheckStatus.WARNING, "ainda não baixada") }
                selfCheckStage = "Conferindo arquivos do rootfs extraído no disco..."
                val rootfsText = withContext(Dispatchers.IO) { runCatching { factory.inspectExtractedRootfs() }.getOrElse { "Falha ao inspecionar rootfs: ${it.message}" } }
                val missing = rootfsText.contains("AUSENTE")
                val m = Regex("Total: (\\d+) arquivos, (\\d+) pastas, (\\d+) symlinks, (\\d+) MB").find(rootfsText)
                val detail = m?.let { "${it.groupValues[1]} arquivos, ${it.groupValues[2]} pastas, ${it.groupValues[4]} MB no disco" } ?: "tamanho não determinado"
                selfCheckReport = SelfCheckReport(System.currentTimeMillis(), listOf(SelfCheckSection("Toolchains", toolchainItems), SelfCheckSection("Ferramentas de linha de comando", cliItems), SelfCheckSection("Plugins opcionais instalados (catálogo)", pluginItems), SelfCheckSection("Mini-LLM local", listOf(llmItem)), SelfCheckSection("Rootfs no disco", listOf(SelfCheckItem("Rootfs extraído (caminhos essenciais)", if (missing) SelfCheckStatus.FAILED else SelfCheckStatus.OK, detail)))))
            } finally { selfCheckStage = null; selfCheckRunning = false; phase = if (runtime != null) SandboxPhase.Ready else SandboxPhase.NotReady }
        }
    }

    fun runDiagnostics() { viewModelScope.launch { diagnosticsReport = withContext(Dispatchers.IO) { runCatching { factory.inspectExtractedRootfs() }.getOrElse { "Falha ao inspecionar rootfs: ${it.message}" } } } }

    fun prepareSandbox() {
        if (phase is SandboxPhase.Downloading || phase is SandboxPhase.Preparing) return
        viewModelScope.launch {
            val ready = withContext(Dispatchers.IO) { factory.isRootfsReady() }
            if (!ready) {
                phase = SandboxPhase.Downloading(0, 0)
                val manifests = try { ManifestLoader.loadAll(getApplication()) } catch (e: IllegalStateException) { phase = SandboxPhase.Blocked(e.message ?: "Manifesto inválido"); return@launch }
                val total = manifests.sumOf { it.sizeBytes }; var completed = 0L
                val results = withContext(Dispatchers.IO) { manifests.mapIndexed { i, manifest -> val r = factory.layerResourceManager(i).ensureAvailable(manifest) { d, _ -> phase = SandboxPhase.Downloading(completed + d, total) }; if (r is SandboxResourceManager.DownloadResult.Success) completed += manifest.sizeBytes; r } }
                results.filterIsInstance<SandboxResourceManager.DownloadResult.Failure>().firstOrNull()?.let { phase = SandboxPhase.Blocked(it.reason); return@launch }
            }
            phase = SandboxPhase.Preparing("Extraindo RootFS", 0, 1)
            try {
                runtime?.shutdown()
                val prepared = withContext(Dispatchers.IO) { factory.prepareManagedRuntime(factory.persistentSessionId()) { c, t, s -> phase = SandboxPhase.Preparing(s, c, t) } }
                runtime = prepared
                val dir = File(getApplication<Application>().filesDir, "sandbox")
                brainController = BrainSandboxController(prepared, File(dir, "rootfs"))
                brainIntegration = BrainIntegrationFacade(File(dir, "brain"))
                platform = SandboxPlatform(prepared, File(dir, "workspace"), File(dir, "components.tsv"), File(dir, "services"))
                pluginListVersion++; refreshStatusCache(); refreshPluginAudit(); phase = SandboxPhase.Ready; refreshToolchains()
            } catch (e: Exception) { runtime = null; phase = SandboxPhase.Blocked(e.message ?: "Falha ao preparar o runtime") }
        }
    }

    fun runCommand() {
        val active = runtime ?: return; if (phase != SandboxPhase.Ready) return; val command = commandInput.trim(); if (command.isEmpty()) return
        viewModelScope.launch { phase = SandboxPhase.Running; val e = withContext(Dispatchers.IO) { runCatching { active.execute(listOf("/bin/bash", "-c", command), 60, "/home/sandbox") }.getOrNull() }; if (e != null) { lastExecution = e; lastResult = e.toUiResult() }; phase = SandboxPhase.Ready }
    }

    fun downloadLocalModel() {
        if (localModelProgress != null || localModelReady) return
        viewModelScope.launch {
            localModelError = null
            val manifest = runCatching { LocalModelManifestLoader.load(getApplication()) }.getOrElse { localModelError = it.message ?: "Manifesto da mini-LLM inválido"; return@launch }
            val result = withContext(Dispatchers.IO) { factory.modelResourceManager(manifest.id).ensureAvailable(manifest) { d, t -> localModelProgress = d to t } }
            localModelProgress = null; when (result) { is SandboxResourceManager.DownloadResult.Success -> localModelReady = true; is SandboxResourceManager.DownloadResult.Failure -> localModelError = result.reason }
        }
    }

    /** Regra estrutural: a conversa Android entra no Brain; nenhum executor local é chamado pelo chat. */
    fun sendChatMessage() {
        val prompt = chatInput.trim(); if (prompt.isEmpty() || chatRunning) return
        chatMessages.add(ChatMessage(ChatRole.USER, prompt)); chatInput = ""; chatRunning = true
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) { runCatching { ChatMessage(ChatRole.ASSISTANT, brainApiGateway.complete(prompt).text) }.getOrElse { ChatMessage(ChatRole.ERROR, "Brain não conseguiu responder: ${it.message ?: it.javaClass.simpleName}") } }
            chatMessages.add(response); chatRunning = false
        }
    }
    fun clearChat() { chatMessages.clear() }

    fun apiKeyInput(providerId: String): String = apiKeyInputs.getOrPut(providerId) { apiKeyStore.get(providerId).orEmpty() }
    fun updateApiKeyInput(providerId: String, value: String) { apiKeyInputs[providerId] = value }
    fun hasStoredApiKey(providerId: String): Boolean = !apiKeyStore.get(providerId).isNullOrBlank()
    fun saveApiKey(providerId: String) { apiKeyStore.save(providerId, apiKeyInputs[providerId]?.trim().orEmpty()); apiKeyTestState[providerId] = ApiKeyTestUiState.Idle }
    fun testApiKey(providerId: String, model: ApiProviderModel) {
        val key = apiKeyStore.get(providerId)?.takeIf { it.isNotBlank() } ?: run { apiKeyTestState[providerId] = ApiKeyTestUiState.Failure("Cole e salve uma chave antes de testar."); return }
        apiKeyTestState[providerId] = ApiKeyTestUiState.Testing
        viewModelScope.launch { val o = withContext(Dispatchers.IO) { ApiKeyTester.test(model.endpoint, model.id, key) }; apiKeyTestState[providerId] = when (o) { is ApiKeyTestOutcome.Success -> ApiKeyTestUiState.Success("Chave OK — HTTP ${o.statusCode} em ${o.latencyMs} ms"); is ApiKeyTestOutcome.Failure -> ApiKeyTestUiState.Failure(o.message) } }
    }
    fun cancelCommand() { viewModelScope.launch(Dispatchers.IO) { runtime?.cancel() } }
    fun runBrainHealthCheck() { val c = brainController ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; lastBrainCycle = withContext(Dispatchers.IO) { runCatching { c.healthCheck(factory.persistentSessionId()) }.getOrNull() }; phase = SandboxPhase.Ready } }
    fun runTestLab(projectPath: String = "/home/sandbox/workspace") { val p = platform ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; lastTestLabReport = withContext(Dispatchers.IO) { runCatching { p.testLab.run(projectPath) }.getOrNull() }; phase = SandboxPhase.Ready } }
    fun runSecurityAssessment() { val p = platform ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; lastSecurityAssessment = withContext(Dispatchers.IO) { runCatching { val root = File(getApplication<Application>().filesDir, "sandbox/workspace"); val scan = p.securityScanner.scan(root); p.security.evaluate(scan, p.securityScenarios, emptyList()) }.getOrNull() }; phase = SandboxPhase.Ready } }
    fun refreshToolchains() { val p = platform ?: return; viewModelScope.launch(Dispatchers.IO) { val s = com.sandbox.sandbox.BuiltInToolchains.all.associate { it.id to runCatching { p.toolchains.refreshStatus(it.id) }.getOrElse { e -> ToolchainStatus(it.id, com.sandbox.sandbox.ToolchainState.FAILED, error = e.message ?: e.javaClass.simpleName) } }; withContext(Dispatchers.Main) { toolchainStatuses = s } } }
    fun installToolchain(id: String) { val p = platform ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; val s = withContext(Dispatchers.IO) { runCatching { p.toolchains.install(id) }.getOrNull() }; if (s != null) toolchainStatuses = toolchainStatuses + (id to s); phase = SandboxPhase.Ready } }
    fun requestApprovalDemo() { val c = brainController ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; val plan = c.approvalDemoPlan(); val runId = "approval-${System.currentTimeMillis()}"; val cycle = withContext(Dispatchers.IO) { c.executePlan(plan, runId) }; pendingApprovalPlan = plan; pendingApprovalRunId = runId; pendingApprovalId = cycle.passos.firstOrNull()?.approvalId; lastBrainCycle = cycle; phase = SandboxPhase.Ready } }
    fun approveAndResume() { val c = brainController ?: return; val plan = pendingApprovalPlan ?: return; val runId = pendingApprovalRunId ?: return; val id = pendingApprovalId ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch { phase = SandboxPhase.Running; lastBrainCycle = withContext(Dispatchers.IO) { c.resumePlan(plan, runId, id) }; pendingApprovalId = null; pendingApprovalPlan = null; pendingApprovalRunId = null; phase = SandboxPhase.Ready } }
    fun refreshWorkspace() { val p = platform ?: return; workspaceProjects = p.workspace.listProjects(); sqliteServiceStatus = p.services.status(BuiltInServices.sqlite("/home/sandbox/workspace")) }
    fun createWorkspaceProject() { val p = platform ?: return; workspaceError = null; runCatching { p.workspace.createProject(workspaceProjectName) }.onSuccess { refreshWorkspace() }.onFailure { workspaceError = it.message ?: "Falha ao criar projeto" } }
    fun inspectGitStatus() { val p = platform ?: return; val project = workspaceProjects.firstOrNull { it.name == workspaceProjectName } ?: run { workspaceError = "Crie ou selecione um projeto antes de consultar o Git."; return }; viewModelScope.launch(Dispatchers.IO) { val s = p.git.status("/home/sandbox/workspace/projects/${project.name}"); withContext(Dispatchers.Main) { lastGitStatus = s.stdout.ifBlank { s.stderr } } } }
    fun startSqliteService() { val p = platform ?: return; viewModelScope.launch(Dispatchers.IO) { val s = runCatching { p.services.start(BuiltInServices.sqlite("/home/sandbox/workspace")) }.getOrNull(); withContext(Dispatchers.Main) { sqliteServiceStatus = s } } }
    fun stopSqliteService() { val p = platform ?: return; viewModelScope.launch(Dispatchers.IO) { val s = p.services.stop(BuiltInServices.sqlite("/home/sandbox/workspace")); withContext(Dispatchers.Main) { sqliteServiceStatus = s } } }
    fun refreshBrainCatalogs() { val i = brainIntegration ?: return; brainSkillSummary = i.enabledSkills().map { "${it.manifest.id} (${it.manifest.capabilities.joinToString()})" }; viewModelScope.launch(Dispatchers.IO) { val r = i.memoryRate(); withContext(Dispatchers.Main) { memorySuccessRate = r } } }
    fun runBrainWorkflow() { val i = brainIntegration ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch(Dispatchers.IO) { val r = runCatching { i.runHealthWorkflow("workflow-${System.currentTimeMillis()}") }.getOrNull(); r?.let { i.recordExperience(it.runId, it.status.name == "COMPLETED") }; withContext(Dispatchers.Main) { lastWorkflowStatus = r?.status?.name ?: "FAILED" } } }
    fun runDiscovery() { val i = brainIntegration ?: return; discoverySummary = runCatching { val r = i.discoverBuiltInCandidate(); "${r.radar.accepted} candidato(s) aceito(s), ${r.radar.rejected} rejeitado(s), ${r.workspace.windows.size} janela(s)" }.getOrElse { "Discovery falhou: ${it.message}" } }
    fun publishLocalDelivery() { val i = brainIntegration ?: return; if (phase != SandboxPhase.Ready) return; viewModelScope.launch(Dispatchers.IO) { val root = File(getApplication<Application>().filesDir, "sandbox/workspace"); val s = runCatching { val r = i.publishLocalDelivery(root, "delivery-${System.currentTimeMillis()}"); "Recibo local: ${r.artifacts.size} artefato(s), ${r.artifacts.sumOf { it.bytes }} bytes, ${r.artifacts.firstOrNull()?.sha256?.take(12) ?: "sem arquivos"}" }.getOrElse { "Falha na entrega local: ${it.message ?: "erro desconhecido"}" }; withContext(Dispatchers.Main) { deliverySummary = s } } }
    fun resetSandbox() { viewModelScope.launch { withContext(Dispatchers.IO) { runtime?.reset { factory.purgeAll() }; runtime = null; brainController = null; brainIntegration = null; factory.clearPersistentSession() }; platform = null; installingComponentIds = emptySet(); statusCache = emptyMap(); pluginSnapshots = emptyList(); pluginHistory = emptyList(); pluginListVersion++; lastResult = null; lastExecution = null; diagnosticsReport = null; lastBrainCycle = null; lastTestLabReport = null; lastSecurityAssessment = null; toolchainStatuses = emptyMap(); pendingApprovalId = null; pendingApprovalPlan = null; pendingApprovalRunId = null; workspaceProjects = emptyList(); lastGitStatus = null; sqliteServiceStatus = null; workspaceError = null; brainSkillSummary = emptyList(); lastWorkflowStatus = null; memorySuccessRate = null; discoverySummary = null; deliverySummary = null; localModelProgress = null; localModelReady = false; localModelError = null; selfCheckReport = null; selfCheckStage = null; selfCheckRunning = false; phase = SandboxPhase.NotReady } }
    fun recentExecutions(limit: Int = 20): List<ExecutionLog> = runtime?.getRecentExecutions(limit) ?: emptyList()
    fun pluginComponents(kind: ComponentKind, query: String, installedOnly: Boolean): List<SandboxComponent> { val base = (platform?.plugins?.components() ?: BuiltInCatalog.all).filter { it.kind == kind }; val searched = if (query.isBlank()) base else base.filter { it.name.contains(query, true) || it.description.contains(query, true) || it.id.contains(query, true) }; return if (!installedOnly) searched else searched.filter { statusCache[it.id]?.state == InstallationState.INSTALLED } }
    fun pluginStatus(id: String): InstalledComponent? = statusCache[id]
    private fun refreshStatusCache() { val p = platform ?: run { statusCache = emptyMap(); return }; viewModelScope.launch(Dispatchers.IO) { val snapshot = p.plugins.components().mapNotNull { c -> p.plugins.status(c.id)?.let { c.id to it } }.toMap(); withContext(Dispatchers.Main) { statusCache = snapshot } } }
    fun installComponent(id: String) { val p = platform ?: run { lastPluginError = "Prepare o sandbox antes de instalar componentes."; return }; if (id in installingComponentIds) return; lastPluginError = null; installingComponentIds = installingComponentIds + id; viewModelScope.launch { var result: Result<InstalledComponent>? = null; try { result = withContext(Dispatchers.IO) { runCatching { p.plugins.install(id) } }; result.onFailure { lastPluginError = it.message ?: "Falha ao instalar $id" }; result.getOrNull()?.let { if (it.state == InstallationState.FAILED) lastPluginError = it.error ?: "Falha ao instalar $id" } } finally { installingComponentIds = installingComponentIds - id; val finished = result; finished?.getOrNull()?.let { statusCache = statusCache + (id to it) }; refreshPluginAudit(); pluginListVersion++ } } }
    fun removeComponent(id: String) { val p = platform ?: run { lastPluginError = "Prepare o sandbox antes de remover componentes."; return }; if (id in installingComponentIds) return; lastPluginError = null; installingComponentIds = installingComponentIds + id; viewModelScope.launch { var result: Result<InstalledComponent?>? = null; try { result = withContext(Dispatchers.IO) { runCatching { p.plugins.remove(id) } }; result.onFailure { lastPluginError = it.message ?: "Falha ao remover $id" }; result.getOrNull()?.let { if (it.state == InstallationState.FAILED) lastPluginError = it.error ?: "Falha ao remover $id" } } finally { installingComponentIds = installingComponentIds - id; val finished = result; finished?.getOrNull()?.let { installed -> statusCache = if (installed != null) statusCache + (id to installed) else statusCache - id }; refreshPluginAudit(); pluginListVersion++ } } }
    fun clearPluginError() { lastPluginError = null }
    fun refreshPluginAudit() { val p = platform ?: return; viewModelScope.launch(Dispatchers.IO) { val snapshots = p.plugins.snapshots(); val history = p.plugins.history(); withContext(Dispatchers.Main) { pluginSnapshots = snapshots; pluginHistory = history } } }
    fun rollbackPlugins(version: Long) { val p = platform ?: return; viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { p.plugins.rollback(version) } }.onFailure { lastPluginError = it.message ?: "Falha ao restaurar snapshot v$version" }; refreshStatusCache(); refreshPluginAudit(); pluginListVersion++ } }
    private fun ExecutionLog.toUiResult() = SandboxExecutionResult(stdout = stdout, stderr = stderr, exitCode = exitCode ?: -1, timedOut = timedOut)
}
