package com.sandbox.sandbox

import com.sandbox.runtime.ExecutionLog
import com.sandbox.runtime.SandboxState
import com.sandbox.runtime.TerminationReason
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManagerLifecycleTest {
    @Test
    fun installResolvesDependenciesAndPersistsBothStates() {
        val repository = JsonComponentRepository(tempComponentsFile())
        val executor = ScriptedExecutor()
        val manager = PluginManager(executor, repository, listOf(
            SandboxComponent("java", "Java", "JDK", ComponentKind.PLUGIN, packages = listOf("default-jdk"), validationCommand = listOf("java", "--version")),
            SandboxComponent("android", "Android", "Build", ComponentKind.PLUGIN, dependencies = listOf("java"), packages = listOf("zip"), validationCommand = listOf("zip", "--version"))
        ))

        assertEquals(InstallationState.INSTALLED, manager.install("android").state)
        assertEquals(InstallationState.INSTALLED, repository.get("java")?.state)
        assertEquals(InstallationState.INSTALLED, repository.get("android")?.state)
    }

    @Test
    fun dependencyFailurePreventsTargetFromBeingMarkedInstalled() {
        val repository = JsonComponentRepository(tempComponentsFile())
        val manager = PluginManager(
            ScriptedExecutor(failWhen = { it.firstOrNull() == "java" }),
            repository,
            listOf(
                SandboxComponent("java", "Java", "JDK", ComponentKind.PLUGIN, packages = listOf("default-jdk"), validationCommand = listOf("java", "--version")),
                SandboxComponent("android", "Android", "Build", ComponentKind.PLUGIN, dependencies = listOf("java"), packages = listOf("zip"), validationCommand = listOf("zip", "--version"))
            )
        )

        val result = manager.install("android")

        assertEquals(InstallationState.FAILED, result.state)
        assertTrue(result.error.orEmpty().contains("Dependência java"))
        assertEquals(InstallationState.FAILED, repository.get("java")?.state)
    }

    @Test
    fun successfulRemovalDeletesThePersistedRecord() {
        val repository = JsonComponentRepository(tempComponentsFile())
        val manager = PluginManager(
            ScriptedExecutor(),
            repository,
            listOf(SandboxComponent("tool", "Tool", "Tool", ComponentKind.TOOL, packages = listOf("tool"), validationCommand = listOf("tool", "--version")))
        )
        manager.install("tool")

        assertNull(manager.remove("tool"))
        assertNull(repository.get("tool"))
    }

    @Test
    fun failedRemovalPreservesFailureStateForRetry() {
        val repository = JsonComponentRepository(tempComponentsFile())
        val manager = PluginManager(
            ScriptedExecutor(failWhen = { command ->
                val script = command.getOrNull(2).orEmpty()
                command.firstOrNull() == "bash" && command.getOrNull(1) == "-c" &&
                    "apt-get" in script && " remove " in script
            }),
            repository,
            listOf(SandboxComponent("tool", "Tool", "Tool", ComponentKind.TOOL, packages = listOf("tool"), validationCommand = listOf("tool", "--version")))
        )
        manager.install("tool")

        val result = manager.remove("tool")

        assertNotNull(result)
        assertEquals(InstallationState.FAILED, result?.state)
        assertTrue(result?.error.orEmpty().isNotBlank())
    }

    @Test
    fun cyclicDependenciesAreRejectedAndPersistedAsFailure() {
        val repository = JsonComponentRepository(tempComponentsFile())
        val manager = PluginManager(
            ScriptedExecutor(failWhen = { true }),
            repository,
            listOf(
                SandboxComponent("a", "A", "A", ComponentKind.PLUGIN, dependencies = listOf("b"), packages = listOf("a"), validationCommand = listOf("a")),
                SandboxComponent("b", "B", "B", ComponentKind.PLUGIN, dependencies = listOf("a"), packages = listOf("b"), validationCommand = listOf("b"))
            )
        )

        val result = manager.install("a")

        assertEquals(InstallationState.FAILED, result.state)
        assertTrue(result.error.orEmpty().contains("Dependência"))
        assertEquals(InstallationState.FAILED, repository.get("b")?.state)
    }

    private class ScriptedExecutor(
        private val failWhen: (List<String>) -> Boolean = { false }
    ) : SandboxCommandExecutor {
        private val counter = AtomicInteger()

        override fun execute(command: List<String>, timeoutSeconds: Long, workingDir: String): ExecutionLog {
            val now = System.currentTimeMillis()
            val failed = failWhen(command)
            return ExecutionLog(
                executionId = "exec-${counter.incrementAndGet()}",
                sessionId = "test",
                command = command,
                workingDir = workingDir,
                startedAt = now,
                finishedAt = now,
                durationMs = 0,
                exitCode = if (failed) 1 else 0,
                terminationReason = TerminationReason.PROCESS_EXIT,
                timedOut = false,
                forcedKill = false,
                stdout = "",
                stderr = if (failed) "erro simulado" else "",
                sandboxState = SandboxState.READY
            )
        }
    }
}
