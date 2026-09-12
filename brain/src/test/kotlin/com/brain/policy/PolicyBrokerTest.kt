package com.brain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Espelha reference/braincode-python/tests/test_policy_events.py — mesmos
 * casos, mesma ordem de raciocínio, agora em Kotlin/JUnit.
 */
class PolicyBrokerTest {

    private fun contexto(
        actor: String = "agent",
        riskClass: RiskClass = RiskClass.LOW,
        approval: ApprovalRequired = ApprovalRequired.NONE,
        sandboxRequired: Boolean = true,
        networkAllowed: Boolean = false,
        filesystemRoots: List<String> = emptyList(),
        budget: Map<String, Long> = emptyMap(),
        ttlSeconds: Int = 300,
        expiresAt: String? = null
    ) = PolicyContext(
        runId = "run-1", taskId = "task-1", actor = actor, riskClass = riskClass,
        approval = approval, sandboxRequired = sandboxRequired, networkAllowed = networkAllowed,
        filesystemRoots = filesystemRoots, budget = budget, ttlSeconds = ttlSeconds, expiresAt = expiresAt
    )

    @Test
    fun `deny by default e permite ator registrado`() {
        val broker = PolicyBroker(listOf("read"), mapOf("agent" to listOf("read")))
        val context = contexto()

        assertEquals(Decision.DENY, broker.authorize("agent", "write", "/tmp", context).decision)
        assertEquals(Decision.ALLOW, broker.authorize("agent", "read", "/tmp", context).decision)
    }

    @Test
    fun `pede aprovacao quando o contexto exige`() {
        val broker = PolicyBroker(listOf("read"), mapOf("agent" to listOf("read")))
        val context = contexto(approval = ApprovalRequired.USER)

        val decisao = broker.authorize("agent", "read", "/tmp", context)
        assertEquals(Decision.ASK, decisao.decision)
    }

    @Test
    fun `capacidade nao registrada e negada mesmo para ator conhecido`() {
        val broker = PolicyBroker(emptyList(), mapOf("agent" to listOf("read")))
        val decisao = broker.authorize("agent", "read", "/tmp", contexto())
        assertEquals(Decision.DENY, decisao.decision)
        assertTrue(decisao.reason.contains("not registered"))
    }

    @Test
    fun `capacidade de rede exige networkAllowed`() {
        val broker = PolicyBroker(listOf("network_fetch"), mapOf("agent" to listOf("network_fetch")))
        val negado = broker.authorize("agent", "network_fetch", "https://x", contexto(networkAllowed = false))
        assertEquals(Decision.DENY, negado.decision)

        val permitido = broker.authorize("agent", "network_fetch", "https://x", contexto(networkAllowed = true))
        assertEquals(Decision.ALLOW, permitido.decision)
    }

    @Test
    fun `capacidade de filesystem exige ao menos uma raiz`() {
        val broker = PolicyBroker(listOf("filesystem_write"), mapOf("agent" to listOf("filesystem_write")))
        val negado = broker.authorize("agent", "filesystem_write", "/tmp/x", contexto(filesystemRoots = emptyList()))
        assertEquals(Decision.DENY, negado.decision)

        val permitido = broker.authorize("agent", "filesystem_write", "/tmp/x", contexto(filesystemRoots = listOf("/tmp")))
        assertEquals(Decision.ALLOW, permitido.decision)
    }

    @Test
    fun `risco alto sem sandbox obrigatorio e negado`() {
        val broker = PolicyBroker(listOf("compile"), mapOf("agent" to listOf("compile")))
        val decisao = broker.authorize(
            "agent", "compile", "/tmp",
            contexto(riskClass = RiskClass.HIGH, sandboxRequired = false)
        )
        assertEquals(Decision.DENY, decisao.decision)
    }

    @Test
    fun `budget negativo e rejeitado`() {
        val broker = PolicyBroker(listOf("read"), mapOf("agent" to listOf("read")))
        val decisao = broker.authorize("agent", "read", "/tmp", contexto(budget = mapOf("cpu_ms" to -1L)))
        assertEquals(Decision.DENY, decisao.decision)
    }

    @Test
    fun `contexto expirado e negado`() {
        val broker = PolicyBroker(listOf("read"), mapOf("agent" to listOf("read")))
        val jaExpirado = java.time.Instant.now().minusSeconds(60).toString()
        val decisao = broker.authorize("agent", "read", "/tmp", contexto(expiresAt = jaExpirado))
        assertEquals(Decision.DENY, decisao.decision)
    }

    @Test
    fun `withActorCapability autoriza novo par ator-capacidade sem mutar o broker original`() {
        val original = PolicyBroker()
        val estendido = original.withActorCapability("agent", "read")

        assertEquals(Decision.DENY, original.authorize("agent", "read", "/tmp", contexto()).decision)
        assertEquals(Decision.ALLOW, estendido.authorize("agent", "read", "/tmp", contexto()).decision)
    }

    @Test
    fun `ExecutionAuthorization so nasce de uma decisao ALLOW`() {
        val broker = PolicyBroker(listOf("read"), mapOf("agent" to listOf("read")))

        val negado = broker.authorize("agent", "write", "/tmp", contexto())
        assertNull(ExecutionAuthorization.fromDecision(negado))

        val perguntado = broker.authorize("agent", "read", "/tmp", contexto(approval = ApprovalRequired.USER))
        assertNull(ExecutionAuthorization.fromDecision(perguntado))

        val permitido = broker.authorize("agent", "read", "/tmp", contexto())
        val autorizacao = ExecutionAuthorization.fromDecision(permitido)
        assertNotNull(autorizacao)
        assertEquals("read", autorizacao!!.capability)
        assertEquals(permitido.decisionId, autorizacao.decisionId)
    }
}
