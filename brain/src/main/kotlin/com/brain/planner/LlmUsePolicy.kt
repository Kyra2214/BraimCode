package com.brain.planner

/** Regra econômica: LLM só entra quando a intenção exige raciocínio ou geração. */
object LlmUsePolicy {
    fun shouldUseLlm(classification: IntentClassification): Boolean = classification.requiresLlm
    fun preferredCostsWhenLlmIsNeeded(): Set<com.brain.capability.CostClass> = setOf(
        com.brain.capability.CostClass.FREE,
        com.brain.capability.CostClass.LOW,
        com.brain.capability.CostClass.MEDIUM,
        com.brain.capability.CostClass.HIGH
    )
}
