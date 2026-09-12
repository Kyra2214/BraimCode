package com.brain.core

import com.brain.provider.ProviderClient
import com.brain.provider.ProviderRequest
import com.brain.provider.ProviderResponse
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLLMSecretarioTest {
    private class FakeProvider(private val response: Result<ProviderResponse>) : ProviderClient {
        override fun complete(request: ProviderRequest): Result<ProviderResponse> = response
    }

    @Test
    fun `interpreta JSON do provider local`() = suspendTest {
        val provider = FakeProvider(Result.success(ProviderResponse(200, """
            {"projectType":"app","platform":"android","complexity":"MEDIA",
             "areasInvolvidas":["frontend","dados"],"podeResolverLocal":false}
        """.trimIndent(), 1, "local")))
        val intent = LocalLLMSecretario(provider, "qwen3").interpretarPedido(UserRequest("criar app"))
        assertEquals("app", intent.projectType)
        assertEquals("android", intent.platform)
        assertEquals(Complexity.MEDIA, intent.complexity)
        assertEquals(listOf("frontend", "dados"), intent.areasInvolvidas)
    }

    @Test
    fun `fallback keyword quando provider falha`() = suspendTest {
        val provider = FakeProvider(Result.failure(IllegalStateException("offline")))
        val intent = LocalLLMSecretario(provider, "qwen3").interpretarPedido(UserRequest("criar um app android"))
        assertEquals("app", intent.projectType)
        assertTrue(intent.podeResolverLocal)
    }

    @Test
    fun `resposta invalida nao cria classificacao inventada`() = suspendTest {
        val provider = FakeProvider(Result.success(ProviderResponse(200, "not-json", 1, "local")))
        val intent = LocalLLMSecretario(provider, "qwen3").interpretarPedido(UserRequest("pedido sem keyword"))
        assertEquals("desconhecido", intent.projectType)
        assertTrue(!intent.podeResolverLocal)
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
