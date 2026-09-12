package com.brain.router

/**
 * Equivalente ao LocalAIRouter + IARoutingProfile do IaBrain, agora
 * decidindo sobre ProviderModel + LiveStats em vez de números fixos.
 * A decisão é dinâmica: dois pedidos idênticos podem escolher provedores
 * diferentes se o LiveStats mudou entre um e outro (quota caiu, erro
 * recente, latência subiu).
 */
data class RoutingProfile(
    val providerId: String,
    val modeloId: String,
    val qualityScore: Double,      // 0.0..1.0 — heurística inicial, ajustada pela Memória (Fase D) com o tempo
    val isDefaultProfile: Boolean = true
)

data class RoutingDecision(
    val escolhido: ProviderModel,
    val alternativas: List<ProviderModel>, // ordem de fallback, já considerando LiveStats no momento da decisão
    val motivo: String
)

/**
 * Continua puro do ponto de vista de "não faz a chamada de rede" — mas
 * agora consulta ApiCatalog.statsAtuais() para cada candidato antes de
 * decidir, então a decisão em si depende de estado mutável observado.
 */
interface AIRouter {
    fun decidir(papel: PapelPipeline, catalog: ApiCatalog, profiles: List<RoutingProfile>): RoutingDecision?

    /** Chamado quando o escolhido falha em tempo real — catalog.registrarResultado() já deve ter sido chamado antes disso. */
    fun proximaAlternativa(decisaoAnterior: RoutingDecision): ProviderModel?
}
