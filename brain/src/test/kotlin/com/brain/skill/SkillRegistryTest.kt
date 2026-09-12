package com.brain.skill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillRegistryTest {
    private fun skill(enabled: Boolean = true, trust: TrustLevel = TrustLevel.CORE) = SkillManifest(
        id = "code.analysis", name = "Code Analysis", version = "1.0.0",
        description = "analisa código", category = "development",
        capabilities = setOf("code_analysis"), trustLevel = trust, enabled = enabled,
        sourceId = "builtin"
    )

    @Test fun `registra e busca por capability`() {
        val registry = SkillRegistry()
        registry.register(skill())
        assertEquals(listOf("code.analysis"), registry.findForCapability("code_analysis").map { it.manifest.id })
    }

    @Test(expected = SecurityException::class)
    fun `skill externa não verificada não pode ser ativada`() {
        SkillRegistry().register(skill(trust = TrustLevel.UNTRUSTED))
    }

    @Test fun `revogação impede uso e novo registro`() {
        val registry = SkillRegistry()
        registry.register(skill())
        registry.revoke("code.analysis", "conteúdo inseguro")
        assertFalse(registry.isUsable("code.analysis"))
        assertTrue(registry.get("code.analysis")!!.revoked)
    }
}
