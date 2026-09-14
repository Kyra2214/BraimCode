package com.brain.planner

import com.brain.execution.RiskClass
import com.brain.router.PapelPipeline

/** Decompõe objetivo em funções declarativas; não autoriza nem executa. */
fun interface FunctionSplitter {
    fun split(objetivo: String): List<PassoPlano>
}

/** Splitter determinístico consolidado da lógica que já existia no KeywordPlanner. */
class KeywordFunctionSplitter : FunctionSplitter {
    override fun split(objetivo: String): List<PassoPlano> {
        val texto = objetivo.trim()
        require(texto.isNotBlank()) { "objetivo não pode ser vazio" }
        val normalizado = texto.lowercase()
        val passos = mutableListOf<PassoPlano>()
        if (normalizado.containsAny("pesquisar", "pesquisa", "analisar", "investigar")) {
            passos += PassoPlano(
                "pesquisar", "network.research", "evidência de pesquisa disponível",
                papel = PapelPipeline.PLANEJAMENTO, riskClass = RiskClass.MEDIUM
            )
        }
        if (normalizado.containsAny("escrever", "criar", "gerar", "documento", "relatório", "relatorio")) {
            passos += PassoPlano(
                "produzir", "workspace.write", "artefato produzido",
                dependeDe = passos.map { it.id }, papel = PapelPipeline.ESCRITA_DE_PROMPT,
                riskClass = RiskClass.MEDIUM
            )
        }
        if (normalizado.containsAny("código", "codigo", "programar", "implementar", "compilar", "testar", "teste")) {
            passos += PassoPlano(
                "executar", "sandbox.code", "execução e testes concluídos",
                dependeDe = passos.map { it.id }, papel = PapelPipeline.EXECUCAO_CODIGO,
                riskClass = RiskClass.HIGH
            )
        }
        if (passos.isEmpty()) {
            passos += PassoPlano("entender", "brain.analyze", "objetivo classificado", papel = PapelPipeline.PLANEJAMENTO)
        }
        return passos
    }

    private fun String.containsAny(vararg termos: String) = termos.any { it in this }
}

/** Planner canônico: recebe funções divididas e apenas monta o ExecutionPlan. */
class KeywordPlanner(
    private val splitter: FunctionSplitter = KeywordFunctionSplitter()
) : Planner {
    override suspend fun planejar(objetivo: String): PlanoExecucao {
        val texto = objetivo.trim()
        require(texto.isNotBlank()) { "objetivo não pode ser vazio" }
        return PlanoExecucao(texto, splitter.split(texto))
    }
}
