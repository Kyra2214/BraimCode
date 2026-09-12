package com.brain.planner

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordPlannerTest {
    @Test fun `objetivo de codigo gera execucao`() = suspendTest {
        val plano = KeywordPlanner().planejar("implementar e testar código")
        assertEquals("executar", plano.ordemDeExecucao.last().id)
        assertTrue(plano.passos.any { it.capacidade == "sandbox.code" })
    }

    @Test fun `objetivo desconhecido permanece seguro e local`() = suspendTest {
        val plano = KeywordPlanner().planejar("organizar ideias")
        assertEquals("brain.analyze", plano.passos.single().capacidade)
    }

    private fun suspendTest(block: suspend () -> Unit) {
        var failure: Throwable? = null
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) { failure = result.exceptionOrNull() }
        })
        failure?.let { throw it }
    }
}
