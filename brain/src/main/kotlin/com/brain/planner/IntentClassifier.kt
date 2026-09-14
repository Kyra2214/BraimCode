package com.brain.planner

import com.brain.capability.CapabilityCategory

/** Classificação rápida e determinística; LLM permanece opcional para casos difíceis. */
data class IntentClassification(
    val intent: Intent,
    val categories: Set<CapabilityCategory>,
    val requiresDecomposition: Boolean,
    val requiresLlm: Boolean
)

class FastIntentClassifier {
    fun classify(objective: String): IntentClassification {
        val text = objective.trim()
        require(text.isNotBlank()) { "objetivo não pode ser vazio" }
        val normalized = text.lowercase()
        val compound = listOf(" e ", " depois ", " então ", ";", "audite", "corrija", "teste").count { normalized.contains(it) } >= 2
        val categories = buildSet {
            if (listOf("pesquis", "web", "github", "url").any(normalized::contains)) add(CapabilityCategory.API)
            if (listOf("código", "codigo", "compilar", "testar", "debug").any(normalized::contains)) add(CapabilityCategory.SANDBOX)
            if (listOf("analis", "planej", "resum", "racioc").any(normalized::contains)) add(CapabilityCategory.AGENT)
            if (isEmpty()) add(CapabilityCategory.INTERNAL)
        }
        val requiresLlm = listOf("raciocin", "análise complexa", "analise complexa", "gerar texto", "resumir").any(normalized::contains)
        return IntentClassification(Intent(text), categories, compound, requiresLlm)
    }
}
