package com.brain.prompt

import com.brain.core.Fase
import com.brain.core.Modulo
import com.brain.core.Roadmap
import com.brain.core.Submodulo
import com.brain.core.Tarefa

/**
 * Implementação mínima do PromptGenerator (Fase C). Reúne as duas peças
 * que no IaBrain viviam juntas em PromptGenerationFlow.kt:
 * ContextualPromptGenerator (monta o texto final) e PromptLibraryService
 * (busca template antes de gerar do zero — reuso antes de criação).
 *
 * Taxa de sucesso mínima pra reusar um template salvo em vez de gerar
 * do zero. Abaixo disso, o Brain prefere pagar o custo de gerar de novo
 * a arriscar reaproveitar algo que historicamente não funciona bem.
 */
private const val TAXA_SUCESSO_MINIMA_PARA_REUSO = 0.5

class DefaultPromptGenerator : PromptGenerator {

    override suspend fun gerarPromptsPorRoadmap(roadmap: Roadmap, library: PromptLibrary): List<PromptGerado> {
        val resultado = mutableListOf<PromptGerado>()
        for (fase in roadmap.fases) {
            for (modulo in fase.modulos) {
                for (submodulo in modulo.submodulos) {
                    for (tarefa in submodulo.tarefas) {
                        resultado += gerarParaTarefa(tarefa, fase, modulo, submodulo, roadmap, library)
                    }
                }
            }
        }
        return resultado
    }

    override suspend fun gerarPromptDeCorrecao(tarefa: Tarefa, motivoReprovacao: String, library: PromptLibrary): PromptGerado {
        val texto = buildString {
            appendLine(HEADER_DESENVOLVEDOR)
            appendLine()
            appendLine("CORREÇÃO NECESSÁRIA")
            appendLine("Tarefa: ${tarefa.descricao} (id: ${tarefa.id})")
            appendLine("Motivo da reprovação: $motivoReprovacao")
            appendLine()
            appendLine("Corrija especificamente o que causou a reprovação acima, sem reabrir escopo já aprovado.")
        }.trim()
        return PromptGerado(tarefaId = tarefa.id, texto = texto, origem = "ROUTER_TASK:${tarefa.id}:CORRECAO")
    }

    private suspend fun gerarParaTarefa(
        tarefa: Tarefa,
        fase: Fase,
        modulo: Modulo,
        submodulo: Submodulo,
        roadmap: Roadmap,
        library: PromptLibrary
    ): PromptGerado {
        val contextoBusca = "${fase.nome} ${modulo.nome} ${submodulo.nome} ${tarefa.descricao}"
        val candidatos = library.buscarPorContexto(contextoBusca)
        val templateReusavel = candidatos.firstOrNull { it.taxaSucesso >= TAXA_SUCESSO_MINIMA_PARA_REUSO }

        val texto = if (templateReusavel != null) {
            renderizarComTemplate(tarefa, fase, modulo, submodulo, roadmap, templateReusavel)
        } else {
            gerarDoZero(tarefa, fase, modulo, submodulo, roadmap)
        }

        val origem = if (templateReusavel != null) {
            "ROUTER_TASK:${tarefa.id}:TEMPLATE:${templateReusavel.id}"
        } else {
            "ROUTER_TASK:${tarefa.id}:GERADO"
        }

        return PromptGerado(tarefaId = tarefa.id, texto = texto, origem = origem)
    }

    private fun renderizarComTemplate(
        tarefa: Tarefa,
        fase: Fase,
        modulo: Modulo,
        submodulo: Submodulo,
        roadmap: Roadmap,
        template: PromptTemplate
    ): String = buildString {
        appendLine(HEADER_DESENVOLVEDOR)
        appendLine()
        appendLine("TEMPLATE REUTILIZADO: ${template.id} (taxa de sucesso ${"%.0f".format(template.taxaSucesso * 100)}%)")
        appendLine(template.textoTemplate)
        appendLine()
        appendCabecalhoContexto(fase, modulo, submodulo, roadmap)
        appendLine("TAREFA")
        appendLine(tarefa.descricao)
    }.trim()

    private fun gerarDoZero(
        tarefa: Tarefa,
        fase: Fase,
        modulo: Modulo,
        submodulo: Submodulo,
        roadmap: Roadmap
    ): String = buildString {
        appendLine(HEADER_DESENVOLVEDOR)
        appendLine()
        appendCabecalhoContexto(fase, modulo, submodulo, roadmap)
        appendLine("OBJETIVO")
        appendLine(tarefa.descricao)
        appendLine()
        appendLine("IMPLEMENTAÇÃO")
        appendLine("Executar somente o escopo descrito, preservando a arquitetura existente do projeto ${roadmap.projectIntent.projectType}.")
        appendLine("CRITÉRIOS DE CONCLUSÃO")
        appendLine("Concluir apenas quando o objetivo desta tarefa for atendido e validado.")
    }.trim()

    private fun StringBuilder.appendCabecalhoContexto(fase: Fase, modulo: Modulo, submodulo: Submodulo, roadmap: Roadmap) {
        appendLine("PROJETO")
        appendLine("${roadmap.projectIntent.projectType} (${roadmap.projectIntent.platform ?: "plataforma não informada"})")
        appendLine()
        appendLine("FASE")
        appendLine(fase.nome)
        appendLine()
        appendLine("MÓDULO")
        appendLine(modulo.nome)
        appendLine()
        appendLine("SUBMÓDULO")
        appendLine(submodulo.nome)
        appendLine()
    }

    companion object {
        /** Igual ao DeveloperPromptStandard.HEADER do IaBrain (brain/PromptGenerationFlow.kt). */
        const val HEADER_DESENVOLVEDOR = """MODO DE EXECUÇÃO SILENCIOSA
FASE → MÓDULO → SUBMÓDULO
- Executar sempre do menor peso para o maior peso
- Trabalhar em apenas 1 submódulo por vez
- Documentação obrigatória ao concluir cada submódulo
- Testes somente no fechamento da Fase
- Parar imediatamente ao concluir o escopo
- Economia de tokens
- Preservação da arquitetura existente
- Nenhuma invenção
- Nenhuma narração intermediária

NÃO finalize a tarefa nem considere o trabalho concluído antes de executar os testes e a validação do resultado."""
    }
}
