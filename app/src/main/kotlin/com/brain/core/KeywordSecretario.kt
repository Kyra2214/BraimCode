package com.brain.core

/**
 * Implementação inicial e propositalmente fraca do Secretario (Fase C).
 * Sem LLM local ainda — é um classificador por palavra-chave, herdeiro
 * direto do TextoLivreCommandResolver do IaBrain
 * (brain/TextoLivreCommandResolver.kt). Serve pra validar o fluxo
 * ponta a ponta (Secretário -> Router -> Prompt) enquanto o candidato
 * real (LocalLLMProvider / Qwen3 GGUF, citado no comentário original
 * do stub Secretario.kt) não é integrado.
 *
 * Combinado ficar fraco por enquanto — evolução fica pra depois, não é
 * pra tentar perfeccionar isso agora.
 */
class KeywordSecretario : Secretario {

    override suspend fun interpretarPedido(request: UserRequest): ProjectIntent {
        val texto = request.rawText.lowercase()
        val comando = comandoPara(texto)

        return ProjectIntent(
            originalRequest = request,
            projectType = tipoProjetoPara(comando, texto),
            platform = plataformaPara(texto),
            complexity = complexidadePara(texto),
            areasInvolvidas = areasPara(comando, texto),
            podeResolverLocal = comando != null // reconheceu keyword = confiança mínima pra tentar local
        )
    }

    override suspend fun pesquisarParaClassificacao(intent: ProjectIntent): List<String> {
        // TODO: sem acesso à internet neste esqueleto ainda. Quando existir,
        // só pesquisar quando projectType == "desconhecido" (nenhuma keyword bateu) —
        // pedido claro não deveria gastar uma chamada de pesquisa à toa.
        return emptyList()
    }

    override fun decidirEstrategia(intent: ProjectIntent): Estrategia =
        if (intent.complexity == Complexity.BAIXA && intent.podeResolverLocal) {
            Estrategia.RESOLVER_LOCAL
        } else {
            Estrategia.ACIONAR_API_PLANEJAMENTO
        }

    /** Mapeamento keyword -> comando, igual ao TextoLivreIntent.commandFor() do IaBrain. */
    private fun comandoPara(texto: String): String? = when {
        listOf("debug", "corrigir código", "corrigir codigo", "erro no código", "erro no codigo").any(texto::contains) -> "/debug"
        listOf("teste", "testar código", "testar codigo", "casos de teste").any(texto::contains) -> "/test"
        listOf("revisão técnica", "revisao tecnica", "code review", "revisar código", "revisar codigo").any(texto::contains) -> "/review"
        listOf("pesquisar", "pesquisa", "fontes", "evidências", "evidencias").any(texto::contains) -> "/research"
        listOf(
            "criar aplicativo", "criar um aplicativo", "criar app", "criar um app",
            "desenvolver aplicativo", "desenvolver um aplicativo", "desenvolver app",
            "implementar", "programar", "código", "codigo"
        ).any(texto::contains) -> "/implement"
        listOf("criar uma imagem", "criar imagem", "gerar imagem", "imagem de", "ilustração", "ilustracao").any(texto::contains) -> "/creative"
        listOf("currículo", "curriculo", "documento profissional", "escrever um texto").any(texto::contains) -> "/document"
        listOf("planilha", "analisar uma planilha", "analisar dados", "análise de dados", "analise de dados", "dataset").any(texto::contains) -> "/analyzedata"
        else -> null
    }

    private fun tipoProjetoPara(comando: String?, texto: String): String = when (comando) {
        "/implement" -> if ("app" in texto || "aplicativo" in texto) "app" else "automação"
        "/creative" -> "conteúdo criativo"
        "/document" -> "documento"
        "/analyzedata" -> "análise de dados"
        "/debug", "/test", "/review" -> "manutenção de código"
        "/research" -> "pesquisa"
        else -> "desconhecido" // honesto: não inventa classificação quando nenhuma keyword bateu
    }

    private fun plataformaPara(texto: String): String? = when {
        "android" in texto || "kotlin" in texto -> "android"
        "ios" in texto || "swift" in texto -> "ios"
        "site" in texto || "web" in texto || "react" in texto -> "web"
        else -> null
    }

    /** Heurística grosseira (tamanho do texto como proxy de complexidade) até existir dado real de execução (Fase D). */
    private fun complexidadePara(texto: String): Complexity = when {
        texto.length < 60 -> Complexity.BAIXA
        texto.length < 200 -> Complexity.MEDIA
        else -> Complexity.ALTA
    }

    private fun areasPara(comando: String?, texto: String): List<String> {
        val areas = mutableListOf<String>()
        if (comando in setOf("/implement", "/debug", "/test", "/review")) areas += "backend"
        if ("interface" in texto || "tela" in texto || "ui" in texto) areas += "frontend"
        if ("banco de dados" in texto || "sql" in texto) areas += "dados"
        if (comando == "/creative") areas += "player/multimidia"
        return areas
    }
}
