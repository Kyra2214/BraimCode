package com.sandbox.agent

import com.brain.policy.ExecutionAuthorization
import com.sandbox.runtime.ExecutionLog
import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File
import java.time.Instant

/**
 * Etapa 3 do plano de integração Brain+Sandbox
 * (docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md): a sessão de trabalho que o
 * agente efetivamente usa dentro do "terreno preparado" pela Policy.
 *
 * Só nasce através de [Sandbox.abrirSessao], nunca diretamente — o
 * construtor é público por necessidade de teste (JVM puro, sem Android),
 * mas o caminho real de produção sempre passa pela authorization já
 * validada. O agente nunca fala com [ManagedSandboxRuntime] diretamente,
 * só com esta sessão: é ela quem decide workspace, valida budget/expiração
 * a cada chamada e restringe leitura/escrita a um único diretório.
 *
 * Limitações conscientes desta etapa (ver Etapa 4 — CapabilityResolver,
 * já parcialmente coberta por [rodarCapacidade]/[CapabilityResolver]):
 * - [rodarCapacidade] só cobre o catálogo fixo de scripts que o rootfs já
 *   embute (`sandbox-build`, `sandbox-test`, `sandbox-health`, etc. — ver
 *   [CapabilityResolver.defaultCatalog]). [rodarComando] continua livre
 *   (argv arbitrário) para o que ainda não tem capacidade catalogada.
 * - [ExecutionAuthorization.networkAllowed] é só informativo aqui. O
 *   `proot` não isola namespace de rede (só intercepta syscalls de
 *   filesystem/processo) — bloquear rede de verdade exige outro mecanismo
 *   (iptables local, proxy, ou remover resolv.conf) que ainda não foi
 *   implementado. Não fingimos aplicar o que não aplicamos.
 * - [ExecutionAuthorization.filesystemRoots] também não vira bind mount
 *   extra: o [com.sandbox.runtime.ProotProcessLauncher] hoje só monta
 *   `/dev`, `/proc`, `/sys`, fixos. A sessão restringe leitura/escrita ao
 *   workspace, mas não gerencia roots adicionais ainda.
 * - Orçamento de CPU é aproximado pelo tempo de parede (`durationMs`) de
 *   cada [ExecutionLog], não uso de CPU real — `proot`/`ManagedSandboxRuntime`
 *   não expõem isso hoje.
 */
class AgentSandboxSession(
    private val authorization: ExecutionAuthorization,
    private val runtime: ManagedSandboxRuntime,
    private val workspaceHostDir: File,
    private val workspaceGuestPath: String,
    private val capabilityResolver: CapabilityResolver = CapabilityResolver(),
    private val clock: () -> Instant = Instant::now
) : AutoCloseable {

    enum class Status { OPEN, EXPIRED, BUDGET_EXCEEDED, CLOSED }

    sealed interface CommandOutcome {
        data class Completed(val log: ExecutionLog) : CommandOutcome
        data class Refused(val reason: String) : CommandOutcome
    }

    sealed interface FileOutcome<out T> {
        data class Ok<T>(val value: T) : FileOutcome<T>
        data class Refused(val reason: String) : FileOutcome<Nothing>
    }

    @Volatile private var status: Status = Status.OPEN
    private var cpuMillisUsed: Long = 0L
    private var outputBytesUsed: Long = 0L
    private val lock = Any()

    val sessionStatus: Status get() = status
    val runId: String get() = authorization.runId
    val taskId: String get() = authorization.taskId

    /**
     * Diretório host do workspace desta sessão — usado pelo Ciclo (Etapa 6,
     * `CicloExecucaoPlano`) pra apontar o [com.brain.qa.ExecutorValidacaoProjeto]
     * pro mesmo lugar onde a capacidade acabou de rodar. Exposto como leitura
     * pura: quem recebe isto não ganha `rodarComando`/`escreverArquivo` de
     * volta, só o caminho, então continua sem forma de escapar das checagens
     * desta sessão pra escrever fora do workspace.
     */
    val workspaceHostPath: File get() = workspaceHostDir

    init {
        workspaceHostDir.mkdirs()
        require(workspaceHostDir.isDirectory) {
            "workspace precisa ser um diretório: ${workspaceHostDir.path}"
        }
    }

    /**
     * Roda a capacidade que a [ExecutionAuthorization] desta sessão já
     * autorizou — não é o agente quem escolhe a capacidade, ela já veio
     * fixada da Policy. O [CapabilityResolver] traduz
     * `authorization.capability` + [parametros] num comando real; se a
     * capacidade não estiver no catálogo do resolver, a chamada é recusada
     * aqui, antes de chegar em [rodarComando].
     */
    fun rodarCapacidade(parametros: List<String> = emptyList(), timeoutSeconds: Long = 60): CommandOutcome {
        checkGate()?.let { return CommandOutcome.Refused(it) }
        return when (val resolution = capabilityResolver.resolve(authorization.capability, parametros)) {
            is CapabilityResolver.Resolution.Refused -> CommandOutcome.Refused(resolution.reason)
            is CapabilityResolver.Resolution.Comando -> rodarComando(resolution.argv, timeoutSeconds)
        }
    }

    /**
     * Roda um comando dentro do workspace fixo desta sessão, via o mesmo
     * `proot` que o [ManagedSandboxRuntime] já usa. `workingDir` não é
     * parâmetro aqui de propósito: a sessão é quem decide onde o agente
     * trabalha, o agente não escolhe.
     */
    fun rodarComando(comando: List<String>, timeoutSeconds: Long = 60): CommandOutcome {
        synchronized(lock) {
            checkGate()?.let { return CommandOutcome.Refused(it) }
            val log = runtime.execute(comando, timeoutSeconds = timeoutSeconds, workingDir = workspaceGuestPath)
            cpuMillisUsed += log.durationMs
            outputBytesUsed += log.stdout.toByteArray().size + log.stderr.toByteArray().size
            enforceBudgetAfter()
            return CommandOutcome.Completed(log)
        }
    }

    /** Lê um arquivo relativo ao workspace. Recusa qualquer caminho que escape dele. */
    fun lerArquivo(caminhoRelativo: String): FileOutcome<String> {
        checkGate()?.let { return FileOutcome.Refused(it) }
        val file = resolveInsideWorkspace(caminhoRelativo)
            ?: return FileOutcome.Refused("caminho fora do workspace: $caminhoRelativo")
        if (!file.isFile) return FileOutcome.Refused("arquivo não encontrado: $caminhoRelativo")
        return FileOutcome.Ok(file.readText())
    }

    /** Escreve um arquivo relativo ao workspace, criando diretórios intermediários se preciso. */
    fun escreverArquivo(caminhoRelativo: String, conteudo: String): FileOutcome<Unit> {
        checkGate()?.let { return FileOutcome.Refused(it) }
        val file = resolveInsideWorkspace(caminhoRelativo)
            ?: return FileOutcome.Refused("caminho fora do workspace: $caminhoRelativo")
        val bytes = conteudo.toByteArray()
        authorization.budget.maxArtifactBytes?.let { max ->
            if (bytes.size > max) return FileOutcome.Refused("arquivo excede maxArtifactBytes ($max)")
        }
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return FileOutcome.Ok(Unit)
    }

    /** Lista entradas (nomes) de um diretório relativo ao workspace. */
    fun listarArquivos(caminhoRelativo: String = "."): FileOutcome<List<String>> {
        checkGate()?.let { return FileOutcome.Refused(it) }
        val dir = resolveInsideWorkspace(caminhoRelativo)
            ?: return FileOutcome.Refused("caminho fora do workspace: $caminhoRelativo")
        if (!dir.isDirectory) return FileOutcome.Refused("não é um diretório: $caminhoRelativo")
        return FileOutcome.Ok(dir.list()?.sorted().orEmpty())
    }

    /**
     * Encerra a sessão. Não derruba o [ManagedSandboxRuntime] (que pode ser
     * compartilhado por outras sessões futuras) — só marca esta sessão como
     * CLOSED, recusando qualquer chamada seguinte.
     */
    override fun close() {
        status = Status.CLOSED
    }

    private fun checkGate(): String? {
        when (status) {
            Status.CLOSED -> return "sessão fechada"
            Status.EXPIRED -> return "autorização expirada em ${authorization.expiresAt}"
            Status.BUDGET_EXCEEDED -> return "orçamento da sessão esgotado"
            Status.OPEN -> Unit
        }
        if (Instant.parse(authorization.expiresAt).isBefore(clock())) {
            status = Status.EXPIRED
            return "autorização expirada em ${authorization.expiresAt}"
        }
        return null
    }

    private fun enforceBudgetAfter() {
        val cpuLimit = authorization.budget.maxCpuMillis
        val outputLimit = authorization.budget.maxOutputBytes
        if ((cpuLimit != null && cpuMillisUsed > cpuLimit) ||
            (outputLimit != null && outputBytesUsed > outputLimit)
        ) {
            status = Status.BUDGET_EXCEEDED
        }
    }

    /** Resolve um caminho relativo garantindo que o resultado continua dentro do workspace. */
    private fun resolveInsideWorkspace(relativo: String): File? {
        val root = workspaceHostDir.canonicalFile
        val target = File(workspaceHostDir, relativo).canonicalFile
        return if (target == root || target.path.startsWith(root.path + File.separator)) target else null
    }
}
