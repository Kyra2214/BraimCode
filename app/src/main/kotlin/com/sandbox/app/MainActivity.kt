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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            0 -> SandboxValidationScreen(viewModel)
            1 -> PluginsScreen(viewModel, ComponentKind.PLUGIN)
            2 -> PluginsScreen(viewModel, ComponentKind.TOOL)
            else -> OperationsScreen(viewModel)
        }
    }
}

@Composable
fun SandboxValidationScreen(viewModel: SandboxViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusSection(viewModel)
        LocalModelSection(viewModel)
        val phase = viewModel.phase
        if (phase is SandboxPhase.Ready || phase is SandboxPhase.Running) CommandSection(viewModel)
        viewModel.lastResult?.let { ResultSection(it, viewModel.lastExecution) }
        viewModel.diagnosticsReport?.let { DiagnosticsSection(it) }
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
                }
            }
        }
    }
}

@Composable
private fun LocalModelSection(viewModel: SandboxViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mini-LLM local", style = MaterialTheme.typography.titleMedium)
            Text("SmolLM2 135M Instruct (GGUF, quantização Q4_K_M, Apache-2.0)")
            when {
                viewModel.localModelReady -> Text("Modelo baixado e verificado por SHA-256.", color = MaterialTheme.colorScheme.primary)
                viewModel.localModelProgress != null -> {
                    val (downloaded, total) = viewModel.localModelProgress!!
                    Text("Baixando modelo: ${downloaded / (1024 * 1024)} MiB / ${total / (1024 * 1024)} MiB")
                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                else -> Button(onClick = { viewModel.downloadLocalModel() }) { Text("Baixar mini-LLM (~101 MiB)") }
            }
            viewModel.localModelError?.let {
                Text("Falha: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DiagnosticsSection(report: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Diagnóstico do rootfs (sem proot, direto do disco):", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(report))
                    Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                }) { Text("Copiar") }
            }
            Text("Toque e segure o texto pra selecionar só um trecho.", style = MaterialTheme.typography.labelSmall)
            SelectionContainer {
                Text(report, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusSection(viewModel: SandboxViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text("Extraindo rootfs e preparando o runtime gerenciado...")
                    CircularProgressIndicator()
                }
                is SandboxPhase.Ready -> {
                    Text("Sandbox pronto — lifecycle gerenciado ativo.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.resetSandbox() }) { Text("Resetar sandbox") }
                        OutlinedButton(onClick = { viewModel.runDiagnostics() }) { Text("Diagnóstico") }
                    }
                    Button(onClick = { viewModel.runBrainHealthCheck() }) {
                        Text("Verificar pelo Brain")
                    }
                    viewModel.lastBrainCycle?.let { cycle ->
                        val result = cycle.passos.singleOrNull()
                        Text(
                            if (cycle.aprovado) {
                                "Brain → Policy → Sandbox: aprovado"
                            } else {
                                "Brain → Policy → Sandbox: ${result?.motivo ?: "reprovado"}"
                            },
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
    Text("Comandos comuns de projeto:", style = MaterialTheme.typography.labelMedium)
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
    Button(onClick = { viewModel.runCommand() }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
        Text(if (running) "Executando..." else "Executar")
    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                            if (result.stderr.isNotBlank()) {
                                if (isNotEmpty()) append("\n\n")
                                append("stderr:\n"); append(result.stderr)
                            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("stdout:", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(result.stdout))
                        Toast.makeText(context, "stdout copiado", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar") }
                }
                Text("Toque e segure pra selecionar só um trecho", style = MaterialTheme.typography.labelSmall)
                SelectionContainer {
                    Text(result.stdout, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (result.stderr.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("stderr:", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(result.stderr))
                        Toast.makeText(context, "stderr copiado", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar") }
                }
                SelectionContainer {
                    Text(result.stderr, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (result.stdout.isBlank() && result.stderr.isBlank()) Text("(sem saída)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
