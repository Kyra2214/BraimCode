package com.sandbox.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundAgentTest {
    @Test
    fun registry_selects_only_agents_with_required_capabilities() {
        val registry = AgentRegistry(BuiltInAgents.all)
        val research = AgentMission(
            id = "m1",
            objective = "Pesquisar documentação",
            requiredCapabilities = setOf(AgentCapability.TERMINAL_RESEARCH, AgentCapability.GITHUB)
        )
        val code = AgentMission(
            id = "m2",
            objective = "Compilar e testar",
            requiredCapabilities = setOf(AgentCapability.CODE_BUILD, AgentCapability.CODE_TEST)
        )

        assertEquals(listOf("research"), registry.findFor(research).map { it.id })
        assertEquals(listOf("code"), registry.findFor(code).map { it.id })
    }

    @Test
    fun agent_rejects_capability_outside_its_allowlist() {
        val agent = ResearchAgent()
        val mission = AgentMission(
            id = "m3",
            objective = "Escrever arquivo",
            requiredCapabilities = setOf(AgentCapability.FILE_WRITE)
        )

        assertFalse(agent.accepts(mission))
        assertTrue(agent.capabilities.contains(AgentCapability.TERMINAL_RESEARCH))
    }

    @Test
    fun agent_uses_only_declared_capabilities() {
        val calls = mutableListOf<AgentCapability>()
        val context = object : AgentExecutionContext {
            override fun invokeCapability(capability: AgentCapability, parameters: Map<String, String>): AgentEvidence {
                calls += capability
                return AgentEvidence("test", capability.name)
            }
        }
        val mission = AgentMission(
            id = "m4",
            objective = "Pesquisa",
            requiredCapabilities = setOf(AgentCapability.WEB_SEARCH, AgentCapability.GITHUB)
        )

        val result = ResearchAgent().execute(mission, context)

        assertTrue(result.success)
        assertEquals(setOf(AgentCapability.WEB_SEARCH, AgentCapability.GITHUB), calls.toSet())
    }
}
