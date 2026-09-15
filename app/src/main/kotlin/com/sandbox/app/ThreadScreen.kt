package com.sandbox.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sandbox.runtime.SandboxExecutionResult

data class DiffLine(val prefix: Char, val text: String)
data class DiffFile(val path: String, val lines: List<DiffLine>)

sealed interface ThreadEvent {
    data class User(val text: String) : ThreadEvent
    data class Agent(val text: String) : ThreadEvent
    data class Terminal(val result: SandboxExecutionResult, val execution: com.sandbox.runtime.ExecutionLog?) : ThreadEvent
    data class Approval(val id: String) : ThreadEvent
    data class Report(val title: String, val body: String) : ThreadEvent
    data class Diff(val files: List<DiffFile>) : ThreadEvent
    data class System(val text: String, val progress: Float? = null) : ThreadEvent
}

@Composable
fun ThreadScreen(viewModel: SandboxViewModel, onOpenSettings: () -> Unit = {}) {
    var sidebarOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        ThreadTopBar(viewModel, searchOpen = searchOpen, onToggleSidebar = { sidebarOpen = !sidebarOpen }, onOpenSettings = onOpenSettings, onSearch = { searchOpen = !searchOpen })
        if (sidebarOpen) {
            TaskSidebar(viewModel, onClose = { sidebarOpen = false })
        } else {
            if (searchOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { searchOpen = false }, modifier = Modifier.weight(0.28f)) { Text("← Voltar") }
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar na thread") }, modifier = Modifier.weight(0.72f), singleLine = true)
                }
            }
            StatusSection(viewModel)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val events = threadEvents(viewModel, query)
                items(events) { event -> ThreadEventCard(event, viewModel) }
            }
            ThreadComposer(viewModel)
        }
    }
}

private fun threadEvents(viewModel: SandboxViewModel, query: String = ""): List<ThreadEvent> = buildList {
    addAll(viewModel.activeThreadEvents)
    if (viewModel.phase == SandboxPhase.Running && viewModel.liveTerminalOutput.isNotBlank()) {
        add(ThreadEvent.Report("Terminal · ao vivo", viewModel.liveTerminalOutput))
    }
    viewModel.lastExecution?.let { execution ->
        viewModel.lastResult?.let { add(ThreadEvent.Terminal(it, execution)) }
    }
    viewModel.pendingApprovalId?.let { add(ThreadEvent.Approval(it)) }
    if (viewModel.activeThreadEvents.none { it is ThreadEvent.Report && it.title == "TestLab" }) {
        viewModel.lastTestLabReport?.let { add(ThreadEvent.Report("TestLab", "${if (it.success) "PASS" else "FAIL"} — ${it.passed}/${it.steps.size} etapas")) }
    }
    if (viewModel.activeThreadEvents.none { it is ThreadEvent.Report && it.title == "Security gate" }) {
        viewModel.lastSecurityAssessment?.let { add(ThreadEvent.Report("Security gate", "${if (it.readiness.ready) "APROVADO" else "BLOQUEADO"} — ${it.findings.size} achado(s)")) }
    }
    if (viewModel.activeThreadEvents.none { it is ThreadEvent.Report && it.title == "Git status" }) {
        viewModel.lastGitStatus?.let { add(ThreadEvent.Report("Git status", it)) }
    }
    viewModel.diagnosticsReport?.let { add(ThreadEvent.Report("Diagnóstico", it)) }
}.filter { query.isBlank() || eventText(it).contains(query, ignoreCase = true) }

private fun eventText(event: ThreadEvent): String = when (event) {
    is ThreadEvent.User -> event.text
    is ThreadEvent.Agent -> event.text
    is ThreadEvent.System -> event.text
    is ThreadEvent.Terminal -> "${event.execution?.command?.joinToString(" ")} ${event.result.stdout} ${event.result.stderr}"
    is ThreadEvent.Approval -> event.id
    is ThreadEvent.Report -> "${event.title} ${event.body}"
    is ThreadEvent.Diff -> event.files.joinToString(" ") { file -> "${file.path} ${file.lines.joinToString { it.text }}" }
}

@Composable
private fun ThreadTopBar(viewModel: SandboxViewModel, searchOpen: Boolean, onToggleSidebar: () -> Unit, onOpenSettings: () -> Unit, onSearch: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToggleSidebar) { Text("Tarefas") }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text("BrainCode", style = MaterialTheme.typography.titleLarge)
                Text("Converse. Execute. Comprove.", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onSearch) { Text(if (searchOpen) "Fechar busca" else "Buscar") }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(start = 8.dp)) { Text("Configurações") }
            AssistChip(onClick = { viewModel.runDiagnostics() }, label = { Text(phaseLabel(viewModel.phase)) })
        }
    }
}

@Composable
private fun TaskSidebar(viewModel: SandboxViewModel, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tarefas", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { viewModel.createSession() }) { Text("+ Nova") }
                TextButton(onClick = onClose) { Text("Thread") }
            }
        }
        viewModel.sessionSummaries.forEach { session ->
            Card(onClick = { viewModel.switchSession(session.id); onClose() }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleSmall)
                    Text("${session.status.name} · ${session.workspaceProjectName ?: "sem workspace"}", style = MaterialTheme.typography.labelSmall)
                    Text(session.lastEventPreview, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

private fun phaseLabel(phase: SandboxPhase): String = when (phase) {
    SandboxPhase.NotReady -> "Não pronto"
    is SandboxPhase.Downloading -> "Baixando"
    is SandboxPhase.Preparing -> "Preparando"
    SandboxPhase.Ready -> "Pronto"
    SandboxPhase.Running -> "Executando"
    is SandboxPhase.Blocked -> "Bloqueado"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ThreadEventCard(event: ThreadEvent, viewModel: SandboxViewModel) {
    when (event) {
        is ThreadEvent.User -> Card(modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { viewModel.quoteEvent(event) })) { Text(event.text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
        is ThreadEvent.Agent -> Card(modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { viewModel.quoteEvent(event) })) { Column(modifier = Modifier.padding(12.dp)) { Text("Turno do agente", style = MaterialTheme.typography.labelSmall); Text(event.text, style = MaterialTheme.typography.bodyMedium) } }
        is ThreadEvent.System -> Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(event.text, color = if (event.text.contains("bloqueado", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                event.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                if (viewModel.phase is SandboxPhase.Blocked) OutlinedButton(onClick = { viewModel.prepareSandbox() }) { Text("Tentar de novo") }
            }
        }
        is ThreadEvent.Terminal -> Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Terminal · ${if (event.result.exitCode == 0) "sucesso" else "falha"}", style = MaterialTheme.typography.titleSmall)
                event.execution?.let { Text("${it.durationMs} ms · ${it.terminationReason.name}", style = MaterialTheme.typography.labelSmall) }
                event.execution?.let { Text("$ ${it.command.joinToString(" ")}\nexit=${it.exitCode}\n${it.stdout.take(600)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
            }
        }
        is ThreadEvent.Approval -> Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aprovação necessária", color = androidx.compose.ui.graphics.Color(0xFFFFB300), style = MaterialTheme.typography.titleSmall)
                Text("ID: ${event.id.take(24)}…", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { viewModel.approveAndResume() }) { Text("Aprovar e retomar") }
            }
        }
        is ThreadEvent.Report -> Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall)
                Text(event.body.take(1000), fontFamily = if (event.title == "Git status") FontFamily.Monospace else FontFamily.Default, style = MaterialTheme.typography.bodySmall)
            }
        }
        is ThreadEvent.Diff -> Card {
            var expanded by remember(event.files) { mutableStateOf(false) }
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Git diff · ${event.files.size} arquivo(s)", style = MaterialTheme.typography.titleSmall)
                val files = if (expanded) event.files else event.files.take(3)
                files.forEach { file ->
                    Text(file.path, style = MaterialTheme.typography.labelMedium)
                    val lines = if (expanded) file.lines else file.lines.take(12)
                    lines.forEach { line ->
                        Text("${line.prefix}${line.text}", color = if (line.prefix == '+') androidx.compose.ui.graphics.Color(0xFF4CAF50) else if (line.prefix == '-') MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Recolher" else "Mostrar diff completo") }
            }
        }
    }
}

@Composable
private fun ThreadComposer(viewModel: SandboxViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (viewModel.phase == SandboxPhase.Ready) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("/testlab", "/security", "/git status", "/git diff", "/workflow", "/approval demo", "/workspace new", "/sqlite start", "/sqlite stop", "/discovery", "/deliver")) { command ->
                    AssistChip(onClick = { viewModel.chatInput = command }, label = { Text(command) })
                }
            }
        }
        OutlinedTextField(
            value = viewModel.chatInput,
            onValueChange = { viewModel.chatInput = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.chatRunning && viewModel.phase == SandboxPhase.Ready,
            label = { Text(if (viewModel.chatRunning) "Executando…" else "Descreva a tarefa ou use /comando") },
            minLines = 2,
            maxLines = 5
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (viewModel.chatRunning) {
                CircularProgressIndicator()
                OutlinedButton(onClick = { viewModel.cancelCommand() }) { Text("Parar") }
            } else {
                Button(onClick = { viewModel.submitThreadInput() }, enabled = viewModel.chatInput.isNotBlank() && viewModel.phase == SandboxPhase.Ready, modifier = Modifier.fillMaxWidth()) { Text("Executar") }
            }
        }
    }
}
