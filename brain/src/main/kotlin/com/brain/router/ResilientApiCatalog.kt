package com.brain.router

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ResilientApiCatalog(private val delegate: ApiCatalog, private val cooldown: Duration = Duration.ofSeconds(30)) : ApiCatalog {
    private val remaining = ConcurrentHashMap<String, Int>()
    private val cooldownUntil = ConcurrentHashMap<String, Instant>()
    private fun key(p: String, m: String) = "$p::$m"
    override fun listarModelos(): List<ProviderModel> = delegate.listarModelos()
    override fun listarPorPapel(papel: PapelPipeline): List<ProviderModel> = delegate.listarPorPapel(papel).filter { available(it) }
    override fun statsAtuais(providerId: String, modeloId: String): LiveStats? = delegate.statsAtuais(providerId, modeloId)?.copy(quotaRestanteEstimada = remaining[key(providerId, modeloId)])
    override fun registrarResultado(providerId: String, modeloId: String, sucesso: Boolean, latenciaMs: Long, erro: ErroObservado?) {
        val k = key(providerId, modeloId)
        if (erro?.tipo == TipoErro.LIMITE_ATINGIDO || erro?.tipo == TipoErro.TIMEOUT) cooldownUntil[k] = Instant.now().plus(cooldown)
        remaining.compute(k) { _, old -> if (old == null) null else (old - 1).coerceAtLeast(0) }
        delegate.registrarResultado(providerId, modeloId, sucesso, latenciaMs, erro)
    }
    fun reserve(model: ProviderModel): Boolean {
        if (!available(model)) return false
        val k = key(model.providerId, model.modeloId)
        val limit = remaining[k] ?: model.janela.porDia ?: Int.MAX_VALUE
        if (limit <= 0) return false
        remaining[k] = if (limit == Int.MAX_VALUE) limit else limit - 1
        return true
    }
    fun reconcile(model: ProviderModel, success: Boolean, error: TipoErro? = null) {
        if (!success && error == TipoErro.LIMITE_ATINGIDO) cooldownUntil[key(model.providerId, model.modeloId)] = Instant.now().plus(cooldown)
        delegate.registrarResultado(model.providerId, model.modeloId, success, 0L, error?.let { ErroObservado(it, Instant.now()) })
    }
    fun waterfall(papel: PapelPipeline, outcome: (ProviderModel) -> Boolean): ProviderModel? = listarPorPapel(papel).firstOrNull { reserve(it) && outcome(it).also { success -> reconcile(it, success) } }
    private fun available(model: ProviderModel): Boolean = cooldownUntil[key(model.providerId, model.modeloId)]?.isAfter(Instant.now()) != true && (remaining[key(model.providerId, model.modeloId)] ?: (model.janela.porDia ?: Int.MAX_VALUE)) > 0
}
