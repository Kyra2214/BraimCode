package com.sandbox.agent

import com.brain.capability.CapabilityCategory
import com.brain.capability.CapabilityRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRegistryCapabilityTest {
    @Test
    fun `agent registry publica metadados no registry universal`() {
        val registry = CapabilityRegistry()
        AgentRegistry(BuiltInAgents.all).publishTo(registry)

        assertEquals(listOf("agent.code", "agent.research"), registry.all().map { it.id })
        val research = registry.getById("agent.research")!!
        assertEquals(CapabilityCategory.AGENT, research.category)
        assertTrue(research.provides("web_search"))
        assertTrue(research.isDiscoverable())
    }
}
