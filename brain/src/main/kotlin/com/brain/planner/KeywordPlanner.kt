package com.brain.planner

import com.brain.execution.RiskClass
import com.brain.router.PapelPipeline

/** Planner local previsível; não concede permissões nem executa capacidades. */
class KeywordPlanner : Planner {
    override suspend fun planejar(objetivo: String): PlanoExecucao {
        val texto = objetivo.trim()
        require(texto.isNotBlank()) { "objetivo não pode ser vazio" }
        val normalizado = texto.lowercase()
        val passos = mutableListOf<PassoPlano>()
        if (normalizado.containsAny("pesquisar", "pesquisa", "analisar", "investigar")) {
            passos += PassoPlano("pesquisar", "network.research", "evidência de pesquisa disponível", papel = PapelPipeline.PLANEJAMENTO, riskClass = RiskClass.MEDIUM)
        }
        if (normalizado.containsAny("escrever", "criar", "gerar", "documento", "relatório", "relatorio")) {
            passos += PassoPlano("produzir", "workspace.write", "artefato produzido", dependeDe = passos.map { it.id }, papel = PapelPipeline.ESCRITA_DE_PROMPT, riskClass = RiskClass.MEDIUM)
        }
        if (normalizado.containsAny("código", "codigo", "programar", "implementar", "compilar", "testar", "teste")) {
            val dependencias = passos.map { it.id }
            passos += PassoPlano("executar", "sandbox.code", "execução e testes concluídos", dependeDe = dependencias, papel = PapelPipeline.EXECUCAO_CODIGO, riskClass = RiskClass.HIGH)
        }
        if (passos.isEmpty()) {
            passos += PassoPlano("entender", "brain.analyze", "objetivo classificado", papel = PapelPipeline.PLANEJAMENTO)
        }
        return PlanoExecucao(texto, passos)
    }

    private fun String.containsAny(vararg termos: String) = termos.any { it in this }
}
