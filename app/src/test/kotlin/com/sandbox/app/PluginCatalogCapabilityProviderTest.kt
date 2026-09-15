package com.sandbox.app

import com.sandbox.sandbox.BuiltInCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginCatalogCapabilityProviderTest {
    @Test
    fun `converte todos os componentes built in em capabilities descobriveis`() {
        val definitions = PluginCatalogCapabilityProvider().capabilities().toList()

        assertEquals(BuiltInCatalog.all.size, definitions.size)
        assertEquals(BuiltInCatalog.all.map { "plugin.${it.id}" }.sorted(), definitions.map { it.id }.sorted())
        assertTrue(definitions.all { it.origin == "builtin-plugin-catalog" })
        assertTrue(definitions.all { it.provenance.any { provenance -> provenance.sourceType == "sandbox-built-in-catalog" } })
        assertTrue(definitions.all { it.metadata["componentId"]?.isNotBlank() == true })
    }

    @Test
    fun `provider preserva metadados de instalacao e validacao sem autorizar execucao`() {
        val definition = PluginCatalogCapabilityProvider().capabilities().first { it.id == "plugin.ollama" }

        assertEquals("ollama", definition.metadata["componentId"])
        assertEquals("true", definition.metadata["installable"])
        assertTrue(definition.providedCapabilities.contains("plugin.ollama"))
        assertTrue(definition.requiredPermissions.isEmpty())
    }
}
