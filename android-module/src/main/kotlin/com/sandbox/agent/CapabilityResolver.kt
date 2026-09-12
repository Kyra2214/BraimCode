package com.sandbox.agent

/**
 * Etapa 4 do plano de integração Brain+Sandbox
 * (docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md): traduz `Requisito(capacidade)`
 * num comando real, restrito ao catálogo de ferramentas que o rootfs já
 * embute (`rootfs-builder/agent-scripts/sandbox-*`) — `sandbox-build` e
 * `sandbox-test` já auto-detectam o tipo de projeto (npm/cargo/go/pytest),
 * `sandbox-health`/`sandbox-info`/`sandbox-diagnose` inspecionam o
 * ambiente, `sandbox-clean` limpa tmp/cache, e `sandbox-run` é a válvula de
 * escape explícita pra comando arbitrário (mesmo `exec "$@"` que
 * [AgentSandboxSession.rodarComando] já expõe, só que nomeada como
 * capacidade em vez de lista de argv solta).
 *
 * Deny-by-default: qualquer capacidade fora do catálogo é recusada aqui —
 * "Começa só com o que esse catálogo já cobre" (doc, Etapa 4). Ferramentas
 * instaláveis via [com.sandbox.sandbox.BuiltInCatalog] (Ollama, NDK, Trivy
 * etc., em `:app`) ficam de fora por ora: aquele catálogo governa
 * instalação/remoção de plugins, não capacidades de agente, e `:android-module`
 * não depende de `:app` (a dependência é na direção contrária).
 */
class CapabilityResolver(
    private val catalog: Map<String, CapabilityHandler> = defaultCatalog()
) {
    sealed interface Resolution {
        data class Comando(val argv: List<String>) : Resolution
        data class Refused(val reason: String) : Resolution
    }

    /** Único ponto de tradução capacidade -> comando. Nunca aceita comando pronto do chamador. */
    fun resolve(capacidade: String, parametros: List<String> = emptyList()): Resolution {
        val handler = catalog[capacidade]
            ?: return Resolution.Refused("capacidade fora do catálogo do CapabilityResolver: $capacidade")
        return handler(parametros)
    }

    companion object {
        fun defaultCatalog(): Map<String, CapabilityHandler> = mapOf(
            "sandbox.build" to passthrough("sandbox-build"),
            "sandbox.test" to passthrough("sandbox-test"),
            "sandbox.health" to noArgs("sandbox-health"),
            "sandbox.info" to noArgs("sandbox-info"),
            "sandbox.diagnose" to noArgs("sandbox-diagnose"),
            "sandbox.clean" to noArgs("sandbox-clean"),
            // Válvula de escape explícita — precisa de pelo menos 1 argumento,
            // igual ao script `sandbox-run` (`exec "$@"`) por baixo.
            "sandbox.run" to { params: List<String> ->
                if (params.isEmpty()) {
                    Resolution.Refused("sandbox.run precisa de ao menos um parâmetro (o comando a rodar)")
                } else {
                    Resolution.Comando(listOf("sandbox-run") + params)
                }
            }
        )

        /** Script que não recebe parâmetros do agente (evita injeção de flags inesperadas). */
        private fun noArgs(script: String): CapabilityHandler = { params ->
            if (params.isNotEmpty()) {
                Resolution.Refused("capacidade para '$script' não aceita parâmetros")
            } else {
                Resolution.Comando(listOf(script))
            }
        }

        /** Script que já é seguro repassando args adiante (auto-detecção interna faz a validação). */
        private fun passthrough(script: String): CapabilityHandler = { params ->
            Resolution.Comando(listOf(script) + params)
        }
    }
}

typealias CapabilityHandler = (List<String>) -> CapabilityResolver.Resolution
