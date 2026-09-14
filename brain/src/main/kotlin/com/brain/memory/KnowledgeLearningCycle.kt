package com.brain.memory

/** Ciclo que separa candidato externo de conhecimento validado. */
class KnowledgeLearningCycle(private val memory: KnowledgeMemory = KnowledgeMemoryRegistry.current()) {
    fun recall(problem: String): KnowledgeEntry? = memory.findValidated(problem)

    fun observeExternal(
        problem: String,
        answer: String,
        source: KnowledgeSource?,
        retrievalHints: List<String>,
        tags: List<String>,
        providerConfidence: Double = 0.0
    ): KnowledgeEntry = memory.saveCandidate(
        KnowledgeEntry(
            problem = problem,
            answer = answer,
            source = source,
            retrievalHints = retrievalHints.distinct(),
            tags = tags.distinct(),
            confidence = providerConfidence.coerceIn(0.0, 1.0),
            validated = false
        )
    )

    /** O Critic chama isto somente depois de validar a resposta. */
    fun confirm(knowledgeId: String, confidence: Double, source: KnowledgeSource? = null): KnowledgeEntry? =
        memory.confirm(knowledgeId, confidence, source)

    /** O Critic pode substituir uma resposta errada sem perder a trilha original. */
    fun correct(knowledgeId: String, correctedAnswer: String, confidence: Double): KnowledgeEntry? =
        memory.recordCorrection(knowledgeId, correctedAnswer, confidence)
}
