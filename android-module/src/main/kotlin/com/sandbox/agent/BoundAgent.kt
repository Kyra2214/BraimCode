package com.sandbox.agent

import com.brain.capability.CapabilityAvailability
import com.brain.capability.CapabilityCategory
import com.brain.capability.CapabilityDefinition
import com.brain.capability.CapabilityProvenance
import com.brain.capability.CapabilityRegistry

/**
 * Agent amarrado do BrainCode.
 *
 * Um Agent não possui LLM próprio e não decide objetivos. O Brain entrega
 * uma missão já definida e o Agent só pode operar dentro das capabilities
 * declaradas pelo seu perfil. Raciocínio, planejamento e seleção de Agent
 * continuam sendo responsabilidades do Brain.
 */
interface BoundAgent {
    val id: String
    val capabilities: Set<AgentCapability>

    fun accepts(mission: AgentMission): Boolean =
        mission.requiredCapabilities.all { it in capabilities }

    fun execute(mission: AgentMission, context: AgentExecutionContext): AgentResult
}

enum class AgentCapability {
    TERMINAL_RESEARCH,
    WEB_SEARCH,
    GITHUB,
    FILE_READ,
    FILE_WRITE,
    CODE_BUILD,
    CODE_TEST,
    WORKSPACE,
    GIT,
    MEDIA
}

data class AgentMission(
    val id: String,
    val objective: String,
    val requiredCapabilities: Set<AgentCapability>,
    val parameters: Map<String, String> = emptyMap()
)

data class AgentEvidence(
    val kind: String,
    val value: String,
    val source: String? = null
)

data class AgentResult(
    val agentId: String,
    val missionId: String,
    val success: Boolean,
    val summary: String,
    val evidence: List<AgentEvidence> = emptyList()
)

/**
 * Context mínimo fornecido pelo Brain/host. O Agent recebe apenas as
 * capabilities que a política autorizou; nenhum comando shell arbitrário
 * é inferido pelo Agent.
 */
interface AgentExecutionContext {
    fun invokeCapability(
        capability: AgentCapability,
        parameters: Map<String, String>
    ): AgentEvidence
}

class AgentRegistry(agents: List<BoundAgent>) {
    private val byId = agents.groupBy { it.id }.also { groups ->
        require(groups.values.all { it.size == 1 }) { "ids de Agent duplicados" }
    }.mapValues { it.value.single() }

    fun get(id: String): BoundAgent? = byId[id]

    fun findFor(mission: AgentMission): List<BoundAgent> =
        byId.values.filter { it.accepts(mission) }

    fun ids(): List<String> = byId.keys.sorted()

    /**
     * Publica os agents bounded no registry universal. O mapa local continua
     * sendo o índice de objetos executáveis; o registry universal é a fonte
     * declarativa para descoberta e não concede autorização.
     */
    fun publishTo(registry: CapabilityRegistry) {
        byId.values.forEach { agent ->
            registry.register(
                CapabilityDefinition(
                    id = "agent.${agent.id}",
                    name = agent.id,
                    description = "Agent bounded sem LLM próprio",
                    category = CapabilityCategory.AGENT,
                    ownerId = agent.id,
                    origin = "agent-registry",
                    providedCapabilities = agent.capabilities.map { it.name.lowercase() }.toSet(),
                    availability = CapabilityAvailability.AVAILABLE,
                    provenance = listOf(
                        CapabilityProvenance(
                            sourceId = agent.id,
                            sourceType = "bounded-agent",
                            evidence = "mission + allowed capabilities"
                        )
                    )
                )
            )
        }
    }
}

/** Agente de pesquisa: somente capabilities de pesquisa, sem LLM. */
class ResearchAgent : BoundAgent {
    override val id: String = "research"
    override val capabilities: Set<AgentCapability> = setOf(
        AgentCapability.TERMINAL_RESEARCH,
        AgentCapability.WEB_SEARCH,
        AgentCapability.GITHUB
    )

    override fun execute(mission: AgentMission, context: AgentExecutionContext): AgentResult {
        if (!accepts(mission)) {
            return AgentResult(id, mission.id, false, "Missão exige capabilities não autorizadas.")
        }
        val evidence = mission.requiredCapabilities.map { capability ->
            context.invokeCapability(capability, mission.parameters)
        }
        return AgentResult(id, mission.id, evidence.isNotEmpty(), "Pesquisa executada pelo Agent amarrado.", evidence)
    }
}

/** Agente de código: execução controlada, sem motor de linguagem próprio. */
class CodeAgent : BoundAgent {
    override val id: String = "code"
    override val capabilities: Set<AgentCapability> = setOf(
        AgentCapability.FILE_READ,
        AgentCapability.FILE_WRITE,
        AgentCapability.WORKSPACE,
        AgentCapability.CODE_BUILD,
        AgentCapability.CODE_TEST,
        AgentCapability.GIT
    )

    override fun execute(mission: AgentMission, context: AgentExecutionContext): AgentResult {
        if (!accepts(mission)) {
            return AgentResult(id, mission.id, false, "Missão exige capabilities não autorizadas.")
        }
        val evidence = mission.requiredCapabilities.map { capability ->
            context.invokeCapability(capability, mission.parameters)
        }
        return AgentResult(id, mission.id, evidence.isNotEmpty(), "Tarefa de código executada pelo Agent amarrado.", evidence)
    }
}

object BuiltInAgents {
    val all: List<BoundAgent> = listOf(
        ResearchAgent(),
        CodeAgent()
    )
}
