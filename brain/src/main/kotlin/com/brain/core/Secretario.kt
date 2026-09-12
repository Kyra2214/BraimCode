package com.brain.core

/**
 * Parte 1 do fluxo: o secretário — mini LLM local que interpreta o pedido,
 * decide se o Brain resolve sozinho ou precisa acionar uma IA externa via
 * API, e pode pesquisar na internet para classificar melhor o projeto.
 *
 * Candidato natural de implementação: reaproveitar o LocalLLMProvider
 * (Qwen3 GGUF) que já existe no IaBrain.
 */
interface Secretario {

    /** Parte 2: interpreta o texto livre do usuário. */
    suspend fun interpretarPedido(request: UserRequest): ProjectIntent

    /**
     * Pesquisa na internet para ajudar a classificar o projeto quando o
     * conhecimento local não é suficiente. Retorna achados resumidos,
     * não texto bruto de página nenhuma.
     */
    suspend fun pesquisarParaClassificacao(intent: ProjectIntent): List<String>

    /**
     * Decide se o próprio secretário resolve a tarefa (ex.: pedido simples)
     * ou se precisa acionar uma API externa de planejamento (Parte 3).
     */
    fun decidirEstrategia(intent: ProjectIntent): Estrategia
}

enum class Estrategia { RESOLVER_LOCAL, ACIONAR_API_PLANEJAMENTO }
