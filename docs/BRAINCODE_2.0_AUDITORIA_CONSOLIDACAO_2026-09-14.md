# Auditoria de consolidação BrainCode 2.0

**Data:** 2026-09-14  
**Escopo desta fase:** auditoria do HEAD inicial e implementação das fases 1–2 do plano mestre: **Universal Capability Model** e **Capability Registry**.

## Resultado da auditoria

O BrainCode atual possui duas implementações complementares, não duas autoridades equivalentes:

| Área | Componente existente | Estado encontrado | Decisão de consolidação |
|---|---|---|---|
| Catálogo de APIs/modelos | `com.brain.router.ApiCatalog`, `InMemoryApiCatalog`, `DynamicFreeApiCatalog` | Catálogo operacional de provider/modelo, métricas, refresh e fallback | Preservar como catálogo operacional; não transformá-lo em registry universal |
| Registro global de capabilities | Não havia um modelo único para API, provider, agent, skill, tool, command, sandbox e workflow | Função ausente | Criar `com.brain.capability.CapabilityDefinition` e `CapabilityRegistry` |
| Discovery de APIs | `ApiDiscoveryEngine` | Normaliza candidatos externos de modelos e exige proveniência/confiança | Preservar; futura integração deverá registrar definições no registry universal |
| Router/provider | `AIRouter`, `DefaultAIRouter`, `ProviderDispatcher` | Seleciona e executa modelos com métricas de qualidade, confiabilidade, velocidade e falha | Preservar; o router não vira autoridade do catálogo universal |
| Policy | `PolicyBroker`, `PolicyContext`, `ExecutionAuthorization` | Autorização deny-by-default, token e limites de sandbox | Preservar; registry não autoriza execução |
| Skills | `SkillRegistry`, `SkillManifest` | Registro especializado com assinatura, confiança e revogação | Preservar; continua a validação de conteúdo; futura integração publicará metadados no registry universal |
| Sandbox | `CapabilityResolver`, `AgentSandboxSession`, `SandboxExecutor` | Allowlist de capacidades e tradução segura para execução | Preservar; resolver continua executor especializado, sem aceitar comando arbitrário |
| Agents | `BoundAgent`, `AgentRegistry` no módulo Android | Agentes bounded com missão, capabilities permitidas e evidência | Preservar; futura integração deve usar o registry universal para descoberta |
| Planner/workflow | `Planner`, `PlanoExecucao`, `WorkflowEngine` | Plano/DAG e execução com dependências já existentes | Preservar; não criar `Planner2` ou `Workflow2` |
| Memory/knowledge/critic | `ExperienceMemory`, `KnowledgeLearningCycle`, `ConservativeKnowledgeCritic` | Memória, proveniência e validação conservadora já implementadas | Preservar; não promover resposta externa diretamente |

## Decisões de design

O novo `CapabilityDefinition` é a representação declarativa comum. Ele contém identidade, descrição, categoria, owner/origem, parâmetros, dependências, permissões, risco, custo, latência estimada, confiabilidade, suporte a arquivos/web/código/reasoning, disponibilidade, versão, status, providers e proveniência.

O `CapabilityRegistry` é o registry único de metadados. Ele oferece `register`, `update`, `remove`, `getById`, consultas por categoria/capability/risco/custo/provider e `discover` estrutural. O método `discover` nesta fase somente filtra entradas declarativas: não aplica Policy, não chama rede, não escolhe executor e não substitui o futuro Capability Discovery hierárquico.

O `ApiCatalogRegistry` não foi removido porque possui consumidores Android reais e representa estado operacional de modelos descobertos. Ele não é a autoridade de capabilities: sua responsabilidade permanece limitada a `ApiCatalog` e métricas de provider/modelo. A integração futura deverá adaptar entradas de API ao registry universal sem quebrar o gateway existente.

## Componentes novos nesta fase

- `brain/src/main/kotlin/com/brain/capability/CapabilityModel.kt`
- `brain/src/main/kotlin/com/brain/capability/CapabilityRegistry.kt`
- `brain/src/test/kotlin/com/brain/capability/CapabilityRegistryTest.kt`
- este relatório de auditoria

## Componentes preservados

Nenhum componente funcional foi removido. Em particular, foram preservados o catálogo dinâmico de APIs, o router, o `PolicyBroker`, o `SkillRegistry`, os agentes bounded, o `CapabilityResolver` do Sandbox, o Planner, o WorkflowEngine e o ciclo de memória/Critic.

## Próxima fase ordenada

A próxima etapa é **Capability Discovery**, que deverá consumir `CapabilityRegistry`, classificar intenção/categoria, filtrar por Policy e ranquear candidatos por compatibilidade, qualidade, confiabilidade, custo, latência, disponibilidade, contexto e risco. Ela não deve carregar todo o registry no contexto do Brain.
