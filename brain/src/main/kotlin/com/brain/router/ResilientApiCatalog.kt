package com.brain.router

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Stateful quota and fallback layer. Reservations are consumed exactly once. */
class ResilientApiCatalog(
    private val delegate: ApiCatalog,
    private val cooldown: Duration = Duration.ofSeconds(30),
    private val clock: () -> Instant = { Instant.now() }
) : ApiCatalog {
    private data class Window(var minuteAt: Instant, var dayAt: Instant, var minuteUsed: Int = 0, var dayUsed: Int = 0, var reserved: Int = 0)
    private val windows = ConcurrentHashMap<String, Window>()
    private val cooldownUntil = ConcurrentHashMap<String, Instant>()
    private fun key(p: String, m: String) = "$p::$m"

    override fun listarModelos(): List<ProviderModel> = delegate.listarModelos()
    override fun listarPorPapel(papel: PapelPipeline): List<ProviderModel> = delegate.listarPorPapel(papel).filter(::available)

    override fun statsAtuais(providerId: String, modeloId: String): LiveStats? {
        val model = delegate.listarModelos().firstOrNull { it.providerId == providerId && it.modeloId == modeloId }
        val stats = delegate.statsAtuais(providerId, modeloId)
        if (stats != null) return stats.copy(quotaRestanteEstimada = model?.let { remaining(it) })
        if (model == null || !windows.containsKey(key(providerId, modeloId))) return null
        return LiveStats(providerId, modeloId, remaining(model), null, null, null, clock())
    }

    override fun registrarResultado(providerId: String, modeloId: String, sucesso: Boolean, latenciaMs: Long, erro: ErroObservado?) {
        val model = delegate.listarModelos().firstOrNull { it.providerId == providerId && it.modeloId == modeloId }
        if (model != null) consume(model)
        if (erro?.tipo == TipoErro.LIMITE_ATINGIDO || erro?.tipo == TipoErro.TIMEOUT) {
            cooldownUntil[key(providerId, modeloId)] = clock().plus(cooldown)
        }
        delegate.registrarResultado(providerId, modeloId, sucesso, latenciaMs, erro)
    }

    @Synchronized fun reserve(model: ProviderModel): Boolean {
        if (!available(model)) return false
        val window = window(model)
        window.minuteUsed++
        window.dayUsed++
        window.reserved++
        return true
    }

    @Synchronized fun reconcile(model: ProviderModel, success: Boolean, error: TipoErro? = null) {
        val window = window(model)
        if (window.reserved > 0) window.reserved--
        if (!success && error == TipoErro.LIMITE_ATINGIDO) {
            cooldownUntil[key(model.providerId, model.modeloId)] = clock().plus(cooldown)
        }
        delegate.registrarResultado(model.providerId, model.modeloId, success, 0L, error?.let { ErroObservado(it, clock()) })
    }

    fun waterfall(papel: PapelPipeline, outcome: (ProviderModel) -> Boolean): ProviderModel? =
        listarPorPapel(papel).firstOrNull { model ->
            reserve(model) && outcome(model).also { reconcile(model, it) }
        }

    private fun available(model: ProviderModel): Boolean =
        cooldownUntil[key(model.providerId, model.modeloId)]?.isAfter(clock()) != true && remaining(model) > 0

    @Synchronized private fun remaining(model: ProviderModel): Int {
        val window = window(model)
        val minute = model.janela.porMinuto?.let { (it - window.minuteUsed).coerceAtLeast(0) } ?: Int.MAX_VALUE
        val day = model.janela.porDia?.let { (it - window.dayUsed).coerceAtLeast(0) } ?: Int.MAX_VALUE
        return minOf(minute, day)
    }

    private fun consume(model: ProviderModel) {
        val window = window(model)
        if (window.reserved > 0) window.reserved-- else { window.minuteUsed++; window.dayUsed++ }
    }

    private fun window(model: ProviderModel): Window {
        val now = clock()
        val w = windows.computeIfAbsent(key(model.providerId, model.modeloId)) { Window(now, now) }
        if (Duration.between(w.minuteAt, now).toSeconds() >= 60) { w.minuteAt = now; w.minuteUsed = 0 }
        if (Duration.between(w.dayAt, now).toHours() >= 24) { w.dayAt = now; w.dayUsed = 0 }
        return w
    }
}
