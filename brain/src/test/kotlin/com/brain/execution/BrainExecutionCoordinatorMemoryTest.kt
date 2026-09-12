package com.brain.execution

import com.brain.events.InMemoryEventStore
import com.brain.memory.FileExperienceMemory
import com.brain.memory.ResultadoExperiencia
import com.brain.planner.PassoPlano
import com.brain.planner.PlanoExecucao
import com.brain.policy.FileApprovalStore
import com.brain.policy.PolicyBroker
import com.brain.router.DefaultAIRouter
import com.brain.router.InMemoryApiCatalog
import java.nio.file.Files
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainExecutionCoordinatorMemoryTest {
    @Test fun `coordenador registra experiencia corrigida e recupera após reinício`() {
        val memoryFile = Files.createTempFile("brain-experience", ".jsonl").toFile()
        val approvalFile = Files.createTempFile("brain-approval", ".jsonl").toFile()
        try {
            val memory = FileExperienceMemory(memoryFile)
            val coordinator = coordinator(memory, approvalFile)
            val plan = PlanoExecucao("objetivo de teste", listOf(PassoPlano("step-1", "code.run", "resultado")))
            var calls = 0
            val result = coordinator.execute(plan, "run-1", "brain", object : StepExecutor {
                override fun execute(step: PassoPlano, provider: com.brain.router.ProviderModel?): StepAttempt {
                    calls++
                    return if (calls == 1) StepAttempt(false, error = "saída inválida") else StepAttempt(true, "ok")
                }
            })

            assertEquals(CoordinatorStatus.COMPLETED, result.status)
            assertEquals(ResultadoExperiencia.CORRIGIDO_APOS_FALHA, await { memory.buscarPorTarefa("step-1") }.single().resultado)
            assertEquals(ResultadoExperiencia.CORRIGIDO_APOS_FALHA, await { FileExperienceMemory(memoryFile).buscarPorTarefa("step-1") }.single().resultado)
        } finally {
            memoryFile.delete()
            approvalFile.delete()
        }
    }

    @Test fun `falha final fica registrada como falha`() {
        val memoryFile = Files.createTempFile("brain-experience", ".jsonl").toFile()
        val approvalFile = Files.createTempFile("brain-approval", ".jsonl").toFile()
        try {
            val memory = FileExperienceMemory(memoryFile)
            val result = coordinator(memory, approvalFile, maxRetries = 0).execute(
                PlanoExecucao("objetivo", listOf(PassoPlano("step-1", "code.run", "resultado"))),
                "run-2", "brain", object : StepExecutor {
                    override fun execute(step: PassoPlano, provider: com.brain.router.ProviderModel?) = StepAttempt(false, error = "falha")
                }
            )
            assertEquals(CoordinatorStatus.FAILED, result.status)
            assertEquals(ResultadoExperiencia.FALHA, await { memory.buscarPorTarefa("step-1") }.single().resultado)
            assertTrue(await { memory.buscarPorTarefa("step-1") }.single().erros.contains("falha"))
        } finally {
            memoryFile.delete()
            approvalFile.delete()
        }
    }

    private fun coordinator(memory: FileExperienceMemory, approvalFile: java.io.File, maxRetries: Int = 1) =
        BrainExecutionCoordinator(
            policy = PolicyBroker(listOf("code.run"), mapOf("brain" to listOf("code.run"))),
            events = InMemoryEventStore(),
            approvals = FileApprovalStore(approvalFile),
            router = DefaultAIRouter(),
            catalog = InMemoryApiCatalog(emptyList()),
            maxRetries = maxRetries,
            memory = memory
        )

    private fun <T> await(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { completed = result }
        })
        return completed!!.getOrThrow()
    }
}
