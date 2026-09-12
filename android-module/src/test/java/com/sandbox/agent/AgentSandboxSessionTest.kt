package com.sandbox.agent

import com.brain.execution.RiskClass
import com.brain.policy.ApprovalRequired
import com.brain.policy.Decision
import com.brain.policy.ExecutionAuthorization
import com.brain.policy.PolicyDecision
import com.sandbox.runtime.FileExecutionLogRepository
import com.sandbox.runtime.ManagedSandboxRuntime
import com.sandbox.runtime.SandboxProcessLauncher
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test

class AgentSandboxSessionTest {

    private fun authorization(
        runId: String = "run-1",
        expiresAt: String = Instant.now().plusSeconds(300).toString(),
        budget: Map<String, Long> = emptyMap(),
        capability: String = "shell.exec"
    ): ExecutionAuthorization {
        val decision = PolicyDecision(
            decisionId = "decision-1",
            runId = runId,
            taskId = "task-1",
            actor = "agent-1",
            capability = capability,
            riskClass = RiskClass.LOW,
            decision = Decision.ALLOW,
            approvalRequired = ApprovalRequired.NONE,
            sandboxRequired = true,
            networkAllowed = false,
            filesystemRoots = emptyList(),
            budget = budget,
            expiresAt = expiresAt,
            reason = "teste"
        )
        return ExecutionAuthorization.fromDecision(decision)!!
    }

    private fun session(
        rootDir: File,
        authorization: ExecutionAuthorization = authorization(),
        capabilityResolver: CapabilityResolver = CapabilityResolver(),
        clock: () -> Instant = Instant::now
    ): Pair<AgentSandboxSession, File> {
        val logDir = File(rootDir, "logs")
        val runtime = ManagedSandboxRuntime(TestLauncher(rootDir), FileExecutionLogRepository(logDir), sessionId = "session-1")
        val sandbox = Sandbox(runtime = runtime, rootfsDir = rootDir, capabilityResolver = capabilityResolver)
        val opened = sandbox.abrirSessao(authorization)
        return opened to File(rootDir, "home/sandbox/workspace/${authorization.runId}")
    }

    @Test fun `arquivo escrito em um passo e lido no proximo, no mesmo workspace`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val (agentSession, _) = session(root)

            val write = agentSession.escreverArquivo("notas.txt", "primeiro passo")
            assertTrue(write is AgentSandboxSession.FileOutcome.Ok)

            val listed = agentSession.listarArquivos(".")
            assertTrue(listed is AgentSandboxSession.FileOutcome.Ok)
            assertEquals(listOf("notas.txt"), (listed as AgentSandboxSession.FileOutcome.Ok).value)

            val read = agentSession.lerArquivo("notas.txt")
            assertTrue(read is AgentSandboxSession.FileOutcome.Ok)
            assertEquals("primeiro passo", (read as AgentSandboxSession.FileOutcome.Ok).value)
        } finally { root.deleteRecursively() }
    }

    @Test fun `comando rodado na sessao enxerga arquivo escrito antes, no mesmo workspace`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val (agentSession, workspace) = session(root)
            agentSession.escreverArquivo("entrada.txt", "conteudo-x")

            val outcome = agentSession.rodarComando(listOf("cat", "entrada.txt"))
            assertTrue(outcome is AgentSandboxSession.CommandOutcome.Completed)
            val log = (outcome as AgentSandboxSession.CommandOutcome.Completed).log
            assertEquals(0, log.exitCode)
            assertEquals("conteudo-x", log.stdout.trim())
            assertTrue(File(workspace, "entrada.txt").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun `caminho que escapa do workspace e recusado`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val (agentSession, _) = session(root)
            val result = agentSession.lerArquivo("../../fora.txt")
            assertTrue(result is AgentSandboxSession.FileOutcome.Refused)
        } finally { root.deleteRecursively() }
    }

    @Test fun `autorizacao expirada recusa novas chamadas`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            // A autorização precisa nascer válida — fromDecision recusa criar
            // a partir de uma decisão já expirada (é assim que a Policy
            // impede "terreno parcial"). Simulamos a expiração *durante* a
            // sessão via um clock fake, em vez de pré-expirar a decisão.
            val validForFiveSeconds = authorization(expiresAt = Instant.now().plusSeconds(5).toString())
            val logDir = File(root, "logs")
            val runtime = ManagedSandboxRuntime(TestLauncher(root), FileExecutionLogRepository(logDir), sessionId = "session-1")
            val hostDir = File(root, "home/sandbox/workspace/${validForFiveSeconds.runId}")
            val futureClock = { Instant.now().plus(1, ChronoUnit.HOURS) }
            val agentSession = AgentSandboxSession(
                authorization = validForFiveSeconds,
                runtime = runtime,
                workspaceHostDir = hostDir,
                workspaceGuestPath = "/home/sandbox/workspace/${validForFiveSeconds.runId}",
                clock = futureClock
            )

            val result = agentSession.escreverArquivo("x.txt", "y")
            assertTrue(result is AgentSandboxSession.FileOutcome.Refused)
            assertEquals(AgentSandboxSession.Status.EXPIRED, agentSession.sessionStatus)
        } finally { root.deleteRecursively() }
    }

    @Test fun `orcamento de saida esgotado bloqueia chamadas seguintes`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val tightBudget = authorization(budget = mapOf("output_bytes" to 5L))
            val (agentSession, _) = session(root, authorization = tightBudget)

            val first = agentSession.rodarComando(listOf("printf", "0123456789"))
            assertTrue(first is AgentSandboxSession.CommandOutcome.Completed)
            assertEquals(AgentSandboxSession.Status.BUDGET_EXCEEDED, agentSession.sessionStatus)

            val second = agentSession.rodarComando(listOf("echo", "mais"))
            assertTrue(second is AgentSandboxSession.CommandOutcome.Refused)
        } finally { root.deleteRecursively() }
    }

    @Test fun `sessao fechada recusa qualquer chamada seguinte`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val (agentSession, _) = session(root)
            agentSession.close()
            val result = agentSession.listarArquivos(".")
            assertTrue(result is AgentSandboxSession.FileOutcome.Refused)
        } finally { root.deleteRecursively() }
    }

    @Test fun `rodarCapacidade usa a capacidade fixada na autorizacao, nunca uma escolhida pelo agente`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val fakeCatalog = CapabilityResolver(mapOf(
                "sandbox.hello" to { params: List<String> -> CapabilityResolver.Resolution.Comando(listOf("echo", "capacidade-ok") + params) }
            ))
            val auth = authorization(capability = "sandbox.hello")
            val (agentSession, _) = session(root, authorization = auth, capabilityResolver = fakeCatalog)

            val outcome = agentSession.rodarCapacidade()
            assertTrue(outcome is AgentSandboxSession.CommandOutcome.Completed)
            val log = (outcome as AgentSandboxSession.CommandOutcome.Completed).log
            assertEquals(0, log.exitCode)
            assertEquals("capacidade-ok", log.stdout.trim())
        } finally { root.deleteRecursively() }
    }

    @Test fun `rodarCapacidade recusa quando a capacidade autorizada nao esta no catalogo do resolver`() {
        val root = createTempDir(prefix = "agent-session-")
        try {
            val vazio = CapabilityResolver(emptyMap())
            val auth = authorization(capability = "sandbox.nao_catalogado")
            val (agentSession, _) = session(root, authorization = auth, capabilityResolver = vazio)

            val outcome = agentSession.rodarCapacidade()
            assertTrue(outcome is AgentSandboxSession.CommandOutcome.Refused)
            // sessão continua OPEN — recusa de capacidade não é o mesmo que budget/expiração estourados.
            assertEquals(AgentSandboxSession.Status.OPEN, agentSession.sessionStatus)
        } finally { root.deleteRecursively() }
    }

    /** Roda comandos de verdade no host (não em proot) — mesma técnica do restante de :android-module. */
    private class TestLauncher(private val rootDir: File) : SandboxProcessLauncher {
        override fun launch(command: List<String>, workingDir: String): Process {
            val hostDir = File(rootDir, workingDir.removePrefix("/")).apply { mkdirs() }
            return ProcessBuilder(command).directory(hostDir).start()
        }
    }
}
