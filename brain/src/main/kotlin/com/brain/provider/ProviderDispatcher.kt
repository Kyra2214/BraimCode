package com.brain.provider

import com.brain.router.ApiCatalog
import com.brain.router.ProviderModel
import com.brain.router.TipoErro
import java.time.Instant

class ProviderDispatcher(private val catalog: ApiCatalog) {
    fun dispatch(model: ProviderModel, client: ProviderClient, request: ProviderRequest): Result<ProviderResponse> {
        val result = client.complete(request.copy(model = model.modeloId))
        result.onSuccess { response ->
            val ok = response.statusCode in 200..299
            catalog.registrarResultado(model.providerId, model.modeloId, ok, response.latencyMs, if (ok) null else TipoErro.ERRO_SERVIDOR.toObserved())
        }.onFailure {
            catalog.registrarResultado(model.providerId, model.modeloId, false, 0L, TipoErro.DESCONHECIDO.toObserved())
        }
        return result
    }

    private fun TipoErro.toObserved() = com.brain.router.ErroObservado(this, Instant.now())
}
