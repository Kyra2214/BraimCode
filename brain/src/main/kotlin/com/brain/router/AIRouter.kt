package com.brain.router

/**
 * Etapa 5 do plano de integração Brain+Sandbox
 * (docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md): o Router que escolhe
 * provider/modelo por conta do usuário — ele nunca vê essa escolha. Recebe
 * a capacidade/papel definido pelo Planner (Etapa 6) e decide sozinho.
 *
 * Equivalente ao LocalAIRouter + IARoutingProfile do IaBrain, agora
 * decidindo sobre ProviderModel + LiveStats em vez de números fixos.
 * A decisão é dinâmica: dois pedidos idênticos podem escolher provedores
 * diferentes se o LiveStats mudou entre um e outro (quota caiu, erro
 * recente, latência subiu).
 *
 * Limitações conscientes desta etapa, em relação ao `apis.py`/
 * `DynamicApiCatalog` de referência (`reference/braincode-python/`):
 * - Sem reserva de quota (`reserve`/`reconcile`) — [ApiCatalog] só observa
 *   `registrarResultado()`, não impede duas escolhas simultâneas de
 *   estourarem a mesma cota antes do resultado voltar.
 * - Sem cooldown/circuit-breaker automático (o Python abre `cooldown_until`
 *   com backoff exponencial após falha); aqui um provedor com erro recente
 *   só perde pontuação, não fica temporariamente indisponível.
 * - Sem `waterfall()` (tentar automaticamente o próximo candidato até um
 *   funcionar) — [proximaAlternativa] só devolve a próxima opção; quem
 *   decide se tenta de novo é o chamador (Etapa 6 — Planner).
 * - [ApiCatalog]/[InMemoryApiCatalog] não persistem entre reinícios — fica
 *   para a Fase G, junto da Etapa 7 (EventStore/Memory).
 * Nenhum desses itens bloqueia a Etapa 6: o Planner já consegue pedir uma
 * decisão e reagir a falha chamando [proximaAlternativa].
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
