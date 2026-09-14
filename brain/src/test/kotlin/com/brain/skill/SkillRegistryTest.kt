package com.brain.skill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SkillRegistryTest {
    private fun skill(enabled: Boolean = true, trust: TrustLevel = TrustLevel.CORE, source: String = "builtin") = SkillManifest(
        id = "code.analysis", name = "Code Analysis", version = "1.0.0",
        description = "analisa código", category = "development",
        capabilities = setOf("code_analysis"), trustLevel = trust, enabled = enabled,
        sourceId = source
    )

    @Test fun `registra e busca por capability`() {
        val registry = SkillRegistry()
        registry.register(skill())
        assertEquals(listOf("code.analysis"), registry.findForCapability("code_analysis").map { it.manifest.id })
    }

    @Test fun `skill brain-builtin é reconhecida como built-in`() {
        val registry = SkillRegistry()
        registry.register(skill(source = "brain-builtin"))
        assertTrue(registry.isUsable("code.analysis"))
    }

    @Test(expected = SecurityException::class)
    fun `skill externa não verificada não pode ser ativada`() {
        SkillRegistry().register(skill(trust = TrustLevel.UNTRUSTED))
    }

    @Test(expected = SecurityException::class)
    fun `skill externa ativa sem assinatura é rejeitada`() {
        SkillRegistry().register(skill(source = "remote-catalog"))
    }

    @Test fun `revogação impede uso e novo registro`() {
        val registry = SkillRegistry()
        registry.register(skill())
        registry.revoke("code.analysis", "conteúdo inseguro")
        assertFalse(registry.isUsable("code.analysis"))
        assertTrue(registry.get("code.analysis")!!.revoked)
    }

    @Test(expected = SecurityException::class)
    fun `revogação persiste após reinicialização do registry`() {
        val file = File.createTempFile("skill-revocations-", ".tsv")
        try {
            val first = SkillRegistry(revocationFile = file)
            first.register(skill())
            first.revoke("code.analysis", "conteúdo inseguro")
            SkillRegistry(revocationFile = file).register(skill())
        } finally { file.delete() }
    }
}
