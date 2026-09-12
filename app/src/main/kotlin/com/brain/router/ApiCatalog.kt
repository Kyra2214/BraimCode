package com.brain.router

import java.time.Instant

/**
 * Catálogo de APIs gratuitas — dinâmico, não números fixos.
 * Granularidade é por MODELO dentro de um provider (não só o provider),
 * porque limite/latência/sucesso variam por modelo, não só por empresa.
 */
data class ProviderModel(
    val providerId: String,       // ex.: "groq", "gemini", "openrouter", "mistral"
    val modeloId: String,         // ex.: "llama-3.3-70b", "gemini-flash"
    val papeisSugeridos: List<PapelPipeline>,
    val janela: JanelaLimite,     // regra estática do provedor (ex.: 30 req/min) — ponto de partida, não verdade permanente
    val contextoMaximoTokens: Int? = null
)

data class JanelaLimite(
    val porMinuto: Int? = null,
    val porDia: Int? = null,
    val tokensPorDia: Long? = null
)

enum class PapelPipeline { PLANEJAMENTO, ESCRITA_DE_PROMPT, EXECUCAO_CODIGO }

/**
 * Estado observado em tempo real — isto é o que o roteador realmente
 * usa para decidir, não a janela estática declarada pelo provedor.
 */
data class LiveStats(
    val providerId: String,
    val modeloId: String,
    val quotaRestanteEstimada: Int?,   // estimativa, atualizada a cada resposta/erro 429
    val ultimoErro: ErroObservado?,
    val latenciaMediaMs: Long?,
    val taxaSucessoRecente: Double?,   // 0.0..1.0, janela deslizante curta
    val atualizadoEm: Instant
)

data class ErroObservado(
    val tipo: TipoErro,
    val ocorridoEm: Instant
)

enum class TipoErro { LIMITE_ATINGIDO, TIMEOUT, ERRO_SERVIDOR, CHAVE_INVALIDA, DESCONHECIDO }

/**
 * TODO: preencher com os provedores/modelos reais decididos no
 * planejamento. Já existe SEED REAL reaproveitado do IaBrain em
 * app/src/main/assets/ai_api_catalog.json (11 provedores gratuitos,
 * incluindo DeepSeek/Qwen/Moonshot/Z.ai/MiniMax/StepFun/Volcengine/
 * SiliconFlow/ModelScope/Tencent/OpenRouter) — usar como carga inicial
 * em vez de recomeçar a lista do zero. janela é só o ponto de partida —
 * quem manda de verdade na decisão é o LiveStats, atualizado a cada chamada.
 */
interface ApiCatalog {
    fun listarModelos(): List<ProviderModel>
    fun listarPorPapel(papel: PapelPipeline): List<ProviderModel>
    fun statsAtuais(providerId: String, modeloId: String): LiveStats?

    /** Chamado após cada chamada real, sucesso ou falha — é isso que mantém o LiveStats vivo. */
    fun registrarResultado(providerId: String, modeloId: String, sucesso: Boolean, latenciaMs: Long, erro: ErroObservado? = null)
}
