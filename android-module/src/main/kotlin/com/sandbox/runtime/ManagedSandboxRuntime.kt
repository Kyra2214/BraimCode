package com.sandbox.runtime

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Lifecycle-aware execution layer for agent workloads. */
class ManagedSandboxRuntime(
    private val launcher: SandboxProcessLauncher,
    private val repository: ExecutionLogRepository,
    private val sessionId: String = FileExecutionLogRepository.newId(),
    private val maxOutputChars: Int = 256 * 1024,
    private val events: (RuntimeEvent) -> Unit = {},
    private val runtimeEventRepository: RuntimeEventRepository? = null
) : AutoCloseable {
    private val stateRef = AtomicReference(SandboxState.NEW)
    private val active = AtomicReference<ActiveExecution?>(null)
    private val lock = Any()

    val state: SandboxState get() = stateRef.get()
    val currentExecutionId: String? get() = active.get()?.id

    init {
        repository.recoverRunning(sessionId)
        stateRef.set(SandboxState.READY)
    }

    fun execute(command: List<String>, timeoutSeconds: Long = 60, workingDir: String = "/home/sandbox", networkAllowed: Boolean = false): ExecutionLog {
        require(command.isNotEmpty()) { "command não pode ser vazio" }
        require(timeoutSeconds > 0) { "timeoutSeconds deve ser > 0" }
        val id = FileExecutionLogRepository.newId()
        val started = System.currentTimeMillis()
        synchronized(lock) {
            check(stateRef.get() == SandboxState.READY) { "Sandbox não está pronto: ${stateRef.get()}" }
            check(active.get() == null) { "Já existe uma execução em andamento" }
            try {
                val process = launcher.launch(command, workingDir, networkAllowed)
                val a = ActiveExecution(id, process)
                active.set(a)
                stateRef.set(SandboxState.RUNNING)
                val marker = ExecutionLog(id, sessionId, command, workingDir, started, 0L, 0L, null,
                    TerminationReason.RUNTIME_ERROR, false, false, "", "", SandboxState.RUNNING)
                runCatching { repository.save(marker) }.onFailure { emit(RuntimeEventType.PERSISTENCE_ERROR, id, it.message) }
            } catch (t: Throwable) {
                stateRef.set(SandboxState.FAILED)
                emit(RuntimeEventType.PROOT_START_FAILED, id, t.message)
                val now = System.currentTimeMillis()
                val failed = ExecutionLog(id, sessionId, command, workingDir, started, now, now - started,
                    null, TerminationReason.START_FAILED, false, false, "", t.message ?: t.javaClass.simpleName, SandboxState.FAILED)
                runCatching { repository.save(failed) }.onFailure { emit(RuntimeEventType.PERSISTENCE_ERROR, id, it.message) }
                stateRef.set(SandboxState.READY)
                return failed
            }
        }
        return finish(active.get()!!, command, workingDir, started, timeoutSeconds)
    }

    fun cancel(): Boolean = terminateActive(TerminationReason.CANCELLED, RuntimeEventType.PROCESS_CANCELLED)

    fun shutdown(): Boolean {
        synchronized(lock) {
            if (stateRef.get() == SandboxState.CLOSED) return false
            stateRef.set(SandboxState.STOPPING)
        }
        val hadProcess = terminateActive(TerminationReason.SHUTDOWN, null)
        waitForFinish()
        emit(RuntimeEventType.RUNTIME_CLOSE, null, if (hadProcess) "active execution stopped" else null)
        stateRef.set(SandboxState.CLOSED)
        return hadProcess
    }

    fun reopen(): List<ExecutionLog> {
        synchronized(lock) {
            check(active.get() == null) { "Não é seguro reabrir durante execução" }
            stateRef.set(SandboxState.READY)
        }
        return repository.recent(sessionId, 50)
    }

    fun reset(deleteRuntimeFiles: () -> Unit = {}) {
        terminateActive(TerminationReason.SHUTDOWN, null)
        waitForFinish()
        try {
            deleteRuntimeFiles()
            emit(RuntimeEventType.RUNTIME_RESET, null, "runtime files reset; execution history preserved")
            stateRef.set(SandboxState.READY)
        } catch (t: Throwable) {
            stateRef.set(SandboxState.FAILED)
            emit(RuntimeEventType.PERSISTENCE_ERROR, null, t.message)
            throw t
        }
    }

    fun getExecution(executionId: String): ExecutionLog? = repository.get(executionId)
    fun getRecentExecutions(limit: Int = 50): List<ExecutionLog> = repository.recent(sessionId, limit)
    fun getExecutionOutput(executionId: String): Pair<String, String>? = repository.get(executionId)?.let { it.stdout to it.stderr }
    fun getSessionLog(): SessionLog {
        val logs = repository.recent(sessionId, 500)
        return SessionLog(sessionId, logs.minOfOrNull { it.startedAt } ?: System.currentTimeMillis(),
            logs.maxOfOrNull { maxOf(it.finishedAt, it.startedAt) } ?: 0L, logs.size)
    }
    fun getRuntimeEvents(limit: Int = 200): List<RuntimeEvent> = runtimeEventRepository?.recent(limit) ?: emptyList()
    fun recoverInterrupted(executionId: String): ExecutionLog? = repository.markInterruptedRunning(executionId)

    override fun close() { shutdown() }

    private fun finish(a: ActiveExecution, command: List<String>, workingDir: String, started: Long, timeoutSeconds: Long): ExecutionLog {
        val stdout = StringCollector(maxOutputChars) { emit(RuntimeEventType.STREAM_READ_ERROR, a.id, it) }
        val stderr = StringCollector(maxOutputChars) { emit(RuntimeEventType.STREAM_READ_ERROR, a.id, it) }
        val latch = CountDownLatch(2)
        val outThread = stream(a.process.inputStream, stdout, latch)
        val errThread = stream(a.process.errorStream, stderr, latch)
        var forcedKill = a.forcedKill.get()
        try {
            if (!a.process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                a.reason.compareAndSet(null, TerminationReason.TIMEOUT)
                emit(RuntimeEventType.PROCESS_TIMEOUT, a.id, "timeout=${timeoutSeconds}s")
                forcedKill = stopProcess(a.process) || forcedKill
            }
        } catch (_: InterruptedException) {
            a.reason.compareAndSet(null, TerminationReason.INTERRUPTED)
            forcedKill = stopProcess(a.process) || forcedKill
            Thread.currentThread().interrupt()
        } finally {
            if (a.reason.get() == null) a.reason.set(TerminationReason.PROCESS_EXIT)
            if (a.reason.get() != TerminationReason.PROCESS_EXIT && a.process.isAlive) forcedKill = stopProcess(a.process) || forcedKill
            runCatching { latch.await(2, TimeUnit.SECONDS) }
            outThread.joinQuietly(); errThread.joinQuietly()
        }
        val finished = System.currentTimeMillis()
        val reason = a.reason.get() ?: TerminationReason.RUNTIME_ERROR
        val exitCode = if (!a.process.isAlive) runCatching { a.process.exitValue() }.getOrNull() else null
        val (cleanedStderr, verifiedLimits) = verifyResourceLimits(stderr.value())
        if (verifiedLimits == false) emit(RuntimeEventType.RESOURCE_LIMIT_UNVERIFIED, a.id, "requested=${launcher.resourceLimits}")
        val log = ExecutionLog(a.id, sessionId, command, workingDir, started, finished, finished - started,
            if (reason == TerminationReason.PROCESS_EXIT) exitCode else null,
            reason, reason == TerminationReason.TIMEOUT, forcedKill || a.forcedKill.get(),
            stdout.value(), cleanedStderr, SandboxState.READY)
        runCatching { repository.save(log) }.onFailure { emit(RuntimeEventType.PERSISTENCE_ERROR, a.id, it.message) }
        active.compareAndSet(a, null)
        stateRef.set(SandboxState.READY)
        return log
    }

    private fun terminateActive(reason: TerminationReason, event: RuntimeEventType?): Boolean {
        val a = active.get() ?: return false
        a.reason.compareAndSet(null, reason)
        synchronized(lock) { if (stateRef.get() != SandboxState.CLOSED) stateRef.set(SandboxState.STOPPING) }
        event?.let { emit(it, a.id, reason.name) }
        if (!a.process.isAlive) return true
        val forced = stopProcess(a.process)
        if (forced) { a.forcedKill.set(true); emit(RuntimeEventType.PROCESS_FORCED_KILL, a.id, reason.name) }
        return true
    }

    private fun waitForFinish() {
        repeat(30) {
            if (active.get() == null) return
            try { Thread.sleep(100) } catch (_: InterruptedException) { Thread.currentThread().interrupt(); return }
        }
    }

    /**
     * Separa a linha de marcação emitida por
     * [ProotResourceLimits.verifiedPreamble] do stderr real do comando, e
     * confere se o `ulimit` efetivo bateu com [launcher.resourceLimits].
     * Retorna `null` no segundo elemento quando o launcher não pediu
     * nenhum limite (nada para verificar) — nesse caso não há evento a
     * emitir, só o caso `false` (pedido, mas não confirmado) é uma falha.
     */
    private fun verifyResourceLimits(rawStderr: String): Pair<String, Boolean?> {
        val limits = launcher.resourceLimits
        if (!limits.hasLimits()) return rawStderr to null
        val (cleaned, parsed) = ResourceLimitVerification.extract(rawStderr)
        return cleaned to ResourceLimitVerification.matches(limits, parsed)
    }

    private fun stopProcess(process: Process): Boolean {
        val descendants = process.toHandle().descendants().toList()
        if (launcher.processGroupManaged) ProcessTreeTerminator.signalGroup(process.pid(), "TERM")
        descendants.asReversed().forEach { it.destroy() }
        process.destroy()
        if (runCatching { process.waitFor(250, TimeUnit.MILLISECONDS) }.getOrDefault(false)) return false
        if (launcher.processGroupManaged) ProcessTreeTerminator.signalGroup(process.pid(), "KILL")
        descendants.asReversed().forEach { it.destroyForcibly() }
        process.destroyForcibly()
        runCatching { process.waitFor(1, TimeUnit.SECONDS) }
        return true
    }

    private fun stream(input: java.io.InputStream, collector: StringCollector, latch: CountDownLatch): Thread = Thread {
        try { input.bufferedReader().forEachLine { collector.append(it) } }
        catch (t: Throwable) { collector.error(t.message ?: t.javaClass.simpleName) }
        finally { runCatching { input.close() }; latch.countDown() }
    }.apply { isDaemon = true; start() }

    private fun emit(type: RuntimeEventType, id: String?, detail: String?) {
        val event = RuntimeEvent(System.currentTimeMillis(), type, id, detail)
        runCatching { runtimeEventRepository?.append(event) }.onFailure { /* diagnostics must never break execution */ }
        runCatching { events(event) }
    }

    private data class ActiveExecution(
        val id: String,
        val process: Process,
        val reason: AtomicReference<TerminationReason?> = AtomicReference(null),
        val forcedKill: java.util.concurrent.atomic.AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean(false)
    )

    private class StringCollector(private val max: Int, private val onError: (String) -> Unit) {
        private val sb = StringBuilder()
        @Synchronized fun append(line: String) {
            if (sb.length >= max) return
            val remaining = max - sb.length
            if (line.length + 1 <= remaining) sb.append(line).append('\n') else sb.append(line.take((remaining - 1).coerceAtLeast(0)))
        }
        fun error(message: String) = onError(message)
        @Synchronized fun value(): String = if (sb.length >= max) sb.toString() + "\n[output truncated]" else sb.toString()
    }

    private fun Thread.joinQuietly() = runCatching { join(2_000) }
}

private object ProcessTreeTerminator {
    fun signalGroup(pid: Long, signal: String) {
        if (pid <= 0) return
        val kill = listOf("/system/bin/kill", "/usr/bin/kill", "/bin/kill").firstOrNull { java.io.File(it).canExecute() } ?: return
        runCatching { ProcessBuilder(kill, "-$signal", "--", "-$pid").start().waitFor(1, TimeUnit.SECONDS) }
    }
}
