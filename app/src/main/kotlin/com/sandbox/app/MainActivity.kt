package com.sandbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sandbox.runtime.SandboxExecutionResult
import com.sandbox.sandbox.BuiltInToolchains
import com.sandbox.sandbox.ComponentKind

class MainActivity : ComponentActivity() {
    private val viewModel: SandboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SandboxMobileApp(viewModel)
                }
            }
        }
    }
}

private val TAB_TITLES = listOf("Validação", "Plugins", "Ferramentas", "Operações")

@Composable
fun SandboxMobileApp(viewModel: SandboxViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Sandbox Mobile",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)
        )
        TabRow(selectedTabIndex = selectedTab) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> ThreadScreen(viewModel)
            1 -> PluginsScreen(viewModel, ComponentKind.PLUGIN)
            2 -> ToolsAndApiScreen(viewModel)
            else -> OperationsScreen(viewModel)
        }
    }
}

@Composable
private fun ToolsAndApiScreen(viewModel: SandboxViewModel) {
    var showApiKeys by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = !showApiKeys, onClick = { showApiKeys = false }, label = { Text("Ferramentas") })
            FilterChip(selected = showApiKeys, onClick = { showApiKeys = true }, label = { Text("Chaves de API") })
        }
        if (showApiKeys) ApiKeysScreen(viewModel) else PluginsScreen(viewModel, ComponentKind.TOOL)
    }
}

@Composable
fun SandboxValidationScreen(viewModel: SandboxViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusSection(viewModel)
        ChatboxSection(viewModel)
        val phase = viewModel.phase
        if (BuildConfig.DEBUG && (phase is SandboxPhase.Ready || phase is SandboxPhase.Running)) CommandSection(viewModel)
        viewModel.lastResult?.let { ResultSection(it, viewModel.lastExecution) }
        viewModel.diagnosticsReport?.let { DiagnosticsSection(it) }
        viewModel.selfCheckReport?.let { SelfCheckReportSection(it) }
    }
}

@Composable
private fun OperationsScreen(viewModel: SandboxViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Operações unificadas", style = MaterialTheme.typography.titleMedium)
                Text("As ações usam o SandboxPlatform e o executor protegido.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.runTestLab() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Executar TestLab") }
                    OutlinedButton(onClick = { viewModel.runSecurityAssessment() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Avaliar segurança") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.requestApprovalDemo() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Solicitar aprovação") }
                    if (viewModel.pendingApprovalId != null) {
                        Button(onClick = { viewModel.approveAndResume() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Aprovar e retomar") }
                    }
                }
                viewModel.pendingApprovalId?.let { Text("Aprovação pendente: ${it.take(18)}…", style = MaterialTheme.typography.bodySmall) }
                viewModel.lastTestLabReport?.let { report ->
                    Text("TestLab: ${if (report.success) "passou" else "reprovado"} (${report.passed}/${report.steps.size})")
                }
                viewModel.lastSecurityAssessment?.let { assessment ->
                    Text("Security gate: ${if (assessment.readiness.ready) "aprovado" else "bloqueado"}")
                    Text("Arquivos analisados: ${assessment.scan.filesScanned}; achados: ${assessment.findings.size}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Workspace, Git e Services", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = viewModel.workspaceProjectName, onValueChange = { viewModel.workspaceProjectName = it }, label = { Text("Projeto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.createWorkspaceProject() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Criar projeto") }
                    OutlinedButton(onClick = { viewModel.refreshWorkspace() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Atualizar") }
                    OutlinedButton(onClick = { viewModel.inspectGitStatus() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Git status") }
                }
                Text("Projetos: ${viewModel.workspaceProjects.joinToString { it.name }.ifBlank { "nenhum" }}", style = MaterialTheme.typography.bodySmall)
                viewModel.lastGitStatus?.let { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startSqliteService() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Iniciar SQLite") }
                    OutlinedButton(onClick = { viewModel.stopSqliteService() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Parar SQLite") }
                }
                viewModel.sqliteServiceStatus?.let { Text("SQLite: ${if (it.running) "ativo (PID ${it.pid})" else "parado"}") }
                viewModel.workspaceError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Brain: Skills, Workflows, Memory e Discovery", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.refreshBrainCatalogs() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Catálogos") }
                    Button(onClick = { viewModel.runBrainWorkflow() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Workflow") }
                    OutlinedButton(onClick = { viewModel.runDiscovery() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Discovery") }
                    OutlinedButton(onClick = { viewModel.publishLocalDelivery() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Recibo local") }
                }
                viewModel.brainSkillSummary.forEach { Text("Skill: $it", style = MaterialTheme.typography.bodySmall) }
                viewModel.lastWorkflowStatus?.let { Text("Workflow: $it") }
                viewModel.memorySuccessRate?.let { Text("Memory success rate: ${(it * 100).toInt()}%", style = MaterialTheme.typography.bodySmall) }
                viewModel.discoverySummary?.let { Text("Discovery: $it", style = MaterialTheme.typography.bodySmall) }
                viewModel.deliverySummary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Toolchains catalogadas", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { viewModel.refreshToolchains() }, enabled = viewModel.phase == SandboxPhase.Ready) { Text("Atualizar status") }
                BuiltInToolchains.all.forEach { profile ->
                    val status = viewModel.toolchainStatuses[profile.id]
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(profile.displayName)
                        OutlinedButton(onClick = { viewModel.installToolchain(profile.id) }, enabled = viewModel.phase == SandboxPhase.Ready) {
                            Text(status?.state?.name ?: "Verificar")
                        }
                    }
                    status?.versionOutput?.takeIf { it.isNotBlank() }?.let { Text(it.trim(), style = MaterialTheme.typography.bodySmall) }
                    status?.error?.takeIf { it.isNotBlank() }?.let { Text("Erro: ${it.trim()}", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun ChatboxSection(viewModel: SandboxViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chat", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.clearChat() }, enabled = viewModel.chatMessages.isNotEmpty()) { Text("Limpar") }
            }
            Text(
                "Conversa do usuário com o Brain. O Chat não expõe modelo, provider ou motor de inferência.",
                style = MaterialTheme.typography.bodySmall
            )
            if (viewModel.chatMessages.isEmpty()) {
                Text("Nenhuma mensagem ainda.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.chatMessages.forEach { message -> ChatBubble(message) }
                }
            }
            if (viewModel.chatRunning) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Processando…", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = viewModel.chatInput,
                onValueChange = { viewModel.chatInput = it },
                label = { Text("Mensagem") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.chatRunning,
                singleLine = false
            )
            Button(
                onClick = { viewModel.sendChatMessage() },
                enabled = !viewModel.chatRunning && viewModel.chatInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (viewModel.chatRunning) "Enviando..." else "Enviar") }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val (label, color) = when (message.role) {
        ChatRole.USER -> "Você" to MaterialTheme.colorScheme.onSurface
        ChatRole.ASSISTANT -> "Chat" to MaterialTheme.colorScheme.primary
        ChatRole.ERROR -> "Erro" to MaterialTheme.colorScheme.error
    }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        SelectionContainer { Text(message.content, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun DiagnosticsSection(report: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diagnóstico do rootfs (sem proot, direto do disco):", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { clipboard.setText(AnnotatedString(report)); Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show() }) { Text("Copiar") }
            }
            Text("Toque e segure o texto pra selecionar só um trecho.", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(report, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SelfCheckReportSection(report: com.sandbox.sandbox.SelfCheckReport) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val markdown = report.toMarkdown()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Teste geral (relatório .md):", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { clipboard.setText(AnnotatedString(markdown)); Toast.makeText(context, "Markdown copiado", Toast.LENGTH_SHORT).show() }) { Text("Copiar") }
            }
            Text(
                "${report.totalOk}/${report.totalItems} OK" +
                    (if (report.totalWarnings > 0) ", ${report.totalWarnings} aviso(s)" else "") +
                    (if (report.totalFailed > 0) ", ${report.totalFailed} falha(s)" else ""),
                color = if (report.allOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall
            )
            Text("Toque e segure o texto pra selecionar só um trecho.", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(markdown, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun StatusSection(viewModel: SandboxViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (viewModel.namespaceSupport.compatibilityMode) {
                Text(
                    "Modo compatibilidade: user namespaces indisponíveis no kernel. Executando via proot com isolamento reduzido (${viewModel.namespaceSupport.reason}).",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("User namespaces disponíveis; o runtime continuará usando proot por compatibilidade.", style = MaterialTheme.typography.bodySmall)
            }
            when (val phase = viewModel.phase) {
                is SandboxPhase.NotReady -> {
                    Text("Sandbox ainda não preparado.")
                    Button(onClick = { viewModel.prepareSandbox() }) { Text("Preparar sandbox") }
                }
                is SandboxPhase.Downloading -> {
                    Text("Baixando rootfs...")
                    if (phase.totalBytes > 0) {
                        val progress = (phase.bytesDownloaded.toFloat() / phase.totalBytes.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("${phase.bytesDownloaded / 1024} KB / ${phase.totalBytes / 1024} KB")
                    } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is SandboxPhase.Preparing -> {
                    Text(phase.stage)
                    if (phase.totalBytes > 0L) {
                        val progress = (phase.bytesCompleted.toFloat() / phase.totalBytes.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("${(progress * 100).toInt()}% — ${phase.bytesCompleted / (1024 * 1024)} MiB / ${phase.totalBytes / (1024 * 1024)} MiB", style = MaterialTheme.typography.bodySmall)
                    } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Não feche o aplicativo durante esta etapa.", style = MaterialTheme.typography.bodySmall)
                }
                is SandboxPhase.Ready -> {
                    Text("Sandbox pronto — lifecycle gerenciado ativo.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.resetSandbox() }) { Text("Resetar sandbox") }
                        OutlinedButton(onClick = { viewModel.runDiagnostics() }) { Text("Diagnóstico") }
                    }
                    Button(onClick = { viewModel.runFullSelfCheck() }, enabled = !viewModel.selfCheckRunning) { Text("Teste geral") }
                    viewModel.selfCheckStage?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = { viewModel.runBrainHealthCheck() }) { Text("Verificar pelo Brain") }
                    viewModel.lastBrainCycle?.let { cycle ->
                        val result = cycle.passos.singleOrNull()
                        Text(
                            if (cycle.aprovado) "Brain → Policy → Sandbox: aprovado" else "Brain → Policy → Sandbox: ${result?.motivo ?: "reprovado"}",
                            color = if (cycle.aprovado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                is SandboxPhase.Running -> {
                    Text("Executando comando — estado RUNNING")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        OutlinedButton(onClick = { viewModel.cancelCommand() }) { Text("Cancelar") }
                    }
                }
                is SandboxPhase.Blocked -> {
                    Text("Bloqueado: ${phase.reason}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.prepareSandbox() }) { Text("Tentar de novo") }
                }
            }
        }
    }
}

@Composable
private fun CommandSection(viewModel: SandboxViewModel) {
    val running = viewModel.phase is SandboxPhase.Running
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Comandos comuns de projeto:", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { viewModel.clearTerminal() }, enabled = !running) { Text("Limpar") }
    }
    QuickCommandsRow(enabled = !running, onPick = { viewModel.commandInput = it })
    OutlinedTextField(
        value = viewModel.commandInput,
        onValueChange = { viewModel.commandInput = it },
        label = { Text("Comando (bash dentro do rootfs)") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        enabled = !running,
        singleLine = false
    )
    Button(onClick = { viewModel.runCommand() }, enabled = !running, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Executando..." else "Executar") }
}

@Composable
private fun QuickCommandsRow(enabled: Boolean, onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QUICK_COMMANDS) { quickCommand ->
            AssistChip(onClick = { onPick(quickCommand.command) }, enabled = enabled, label = { Text(quickCommand.label) })
        }
    }
}

@Composable
private fun ResultSection(result: SandboxExecutionResult, execution: com.sandbox.runtime.ExecutionLog?) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when {
                        result.timedOut -> "Timeout"
                        result.succeeded -> "Concluído (exit code 0)"
                        else -> "Concluído (exit code ${result.exitCode})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (result.succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (result.stdout.isNotBlank() || result.stderr.isNotBlank()) {
                    TextButton(onClick = {
                        val combined = buildString {
                            if (result.stdout.isNotBlank()) { append("stdout:\n"); append(result.stdout) }
                            if (result.stderr.isNotBlank()) { if (isNotEmpty()) append("\n\n"); append("stderr:\n"); append(result.stderr) }
                        }
                        clipboard.setText(AnnotatedString(combined))
                        Toast.makeText(context, "Saída copiada", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar tudo") }
                }
            }
            execution?.let {
                Text("ID: ${it.executionId}", style = MaterialTheme.typography.bodySmall)
                Text("Terminação: ${it.terminationReason} • ${it.durationMs} ms", style = MaterialTheme.typography.bodySmall)
                if (it.outputTruncated) Text("Saída limitada para proteger a memória do app.", style = MaterialTheme.typography.bodySmall)
            }
            if (result.stdout.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("stdout:", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(result.stdout)); Toast.makeText(context, "stdout copiado", Toast.LENGTH_SHORT).show() }) { Text("Copiar") }
                }
                Text("Toque e segure pra selecionar só um trecho", style = MaterialTheme.typography.labelSmall)
                SelectionContainer { Text(result.stdout, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
            }
            if (result.stderr.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("stderr:", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(result.stderr)); Toast.makeText(context, "stderr copiado", Toast.LENGTH_SHORT).show() }) { Text("Copiar") }
                }
                SelectionContainer { Text(result.stderr, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            if (result.stdout.isBlank() && result.stderr.isBlank()) Text("(sem saída)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
