package com.brain.capability

import com.brain.execution.RiskClass

/** Primeiros agents declarativos previstos no plano; execução continua bounded e policy-gated. */
object BuiltInAgentDefinitions {
    fun researchAgent(): CapabilityDefinition = definition(
        id = "agent.research",
        name = "ResearchAgent",
        description = "Agente bounded para pesquisa com evidência e provenance",
        provided = setOf("research.web", "research.evidence"),
        risk = RiskClass.MEDIUM,
        web = true
    )

    fun codeAgent(): CapabilityDefinition = definition(
        id = "agent.code",
        name = "CodeAgent",
        description = "Agente bounded para análise, implementação e testes em Sandbox",
        provided = setOf("code.edit", "code.test"),
        risk = RiskClass.HIGH,
        code = true
    )

    private fun definition(id: String, name: String, description: String, provided: Set<String>, risk: RiskClass, web: Boolean = false, code: Boolean = false) = CapabilityDefinition(
        id = id, name = name, description = description, category = CapabilityCategory.AGENT,
        ownerId = "brain-builtin", origin = "brain-builtin", providedCapabilities = provided,
        risk = risk, supportsWeb = web, supportsCode = code, availability = CapabilityAvailability.AVAILABLE,
        reliability = .8, quality = .8, provenance = listOf(CapabilityProvenance("brain-builtin", "agent-definition"))
    )
}
