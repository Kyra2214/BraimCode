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

## Fase 3 — Capability Discovery

Foi consolidado `CapabilityDiscovery` sobre o registry único. A descoberta segue `intenção → categoria → candidatas estruturais → filtro de policy → ranking → limite de candidatos`. A inferência de categoria é determinística e o ranking combina compatibilidade, qualidade, confiabilidade, custo, latência, disponibilidade, risco, provider preferido e contexto declarativo. A função de policy é injetada; Discovery não autoriza, executa rede ou escolhe comandos.

## Fase 4 — Policy Broker

O `PolicyBroker` existente foi evoluído, sem duplicação, para consultar o `CapabilityRegistry` como fonte de registro quando fornecido e avaliar actor, capability, recurso, classificação de dados, ambiente, risco, sandbox, rede, filesystem, orçamento, TTL e aprovação. `PolicyOutcome` expõe `ALLOW`, `DENY`, `REQUIRE_APPROVAL` e `SANDBOX_ONLY`; `Decision.ASK` permanece como compatibilidade interna. O broker continua deny-by-default e não executa ações.

## Fase 5 — Action Gateway

Foi criado `com.brain.gateway.ActionGateway` no módulo `:brain` como contrato agnóstico de execução. Ele resolve a capability no registry, consulta o `PolicyBroker`, valida o token/recursos, não aceita comando bruto, chama somente um `ActionExecutor` injetado e registra `actionId`, actor, capability, parâmetros redigidos, decisão, início/fim, resultado, erro, evidence e provenance. `AuthorizedCapabilityExecutor` e `PolicyGatedExecutor` Android foram preservados como backends especializados; nenhum deles foi removido ou contornado.

## Fase 6 — Agent Registry

O `AgentRegistry` bounded existente foi preservado e passou a publicar definições `CapabilityCategory.AGENT` no registry universal por meio de `publishTo`. O índice local continua armazenando objetos executáveis e a publicação é apenas declarativa; missão, allowlist e evidência continuam obrigatórias. O teste do módulo Android não pôde ser executado porque o ambiente não possui Android SDK configurado (`ANDROID_HOME`/`sdk.dir` ausentes); `:brain:test` permaneceu verde.

## Fase 7 — Skill Registry

O `SkillRegistry` especializado existente foi preservado para validação de manifests, assinatura, trust e revogação. Ele passou a publicar somente Skills habilitadas no `CapabilityRegistry` por `publishTo`; a publicação não concede autorização e mantém as capabilities declaradas descobríveis.

## Fase 8 — Retrieval

Foi criado `com.brain.retrieval.Retrieval` sobre contratos existentes de memória, skills, prompt library, agents e APIs. A ordem é fixa e explícita: `MEMORY → SKILLS → PROMPT_LIBRARY → TOOLS → AGENTS → APIS`. Memory e Skills exigem hits validados; o primeiro hit válido encerra a busca e impede chamadas de fontes posteriores. Retrieval não armazena conhecimento nem autoriza execução.

## Fase 9 — Knowledge Compiler

O `KnowledgeCompiler` foi consolidado sobre `KnowledgeLearningCycle` e `ConservativeKnowledgeCritic`. O fluxo explícito é `KnowledgeEvidence → candidate KnowledgeEntry → Critic → validated knowledge`; resposta externa sem fonte permanece incerta. A criação de `SkillCandidate` só ocorre para conhecimento validado, e o registro de Skill exige chamada explícita de validação no `SkillRegistry`.

## Fase 10 — Planner

`PlanoExecucao` foi mantido como contrato canônico e recebeu o alias `ExecutionPlan`, além da coleção derivada de `requiredCapabilities`. O Planner continua produzindo objetivo, passos, dependências e critérios; não executa ações nem escolhe executor.

## Fase 11 — Function Splitter

A lógica já existente do `KeywordPlanner` foi extraída para `FunctionSplitter`/`KeywordFunctionSplitter`. O Planner agora somente solicita funções declarativas e monta `ExecutionPlan`; cada função continua declarando sua capability, dependências, papel, risco e critério de sucesso.

## Fase 12 — Dispatcher

Foi criado `com.brain.dispatch.Dispatcher`, que recebe uma tarefa do `ExecutionPlan`, consulta `CapabilityDiscovery`, escolhe um único candidato e encaminha uma `ActionRequest` ao `ActionGateway`. Ele não redefine estratégia, não autoriza e não executa comandos diretamente.

## Fase 13 — Workflow/DAG

O `WorkflowEngine` existente foi evoluído, sem criar outro motor, para manter DAG/dependências, retry, lease/fencing e idempotência, e acrescentar paralelismo limitado por `maxParallelism`, cancelamento cooperativo, timeout por node, status `CANCELLED`/`TIMED_OUT`, evidências intermediárias e persistência dessas evidências. A assinatura antiga com execute como trailing lambda foi preservada.

## Fase 12 — Dispatcher

Foi criado `com.brain.dispatch.Dispatcher`, que recebe uma tarefa do `ExecutionPlan`, consulta `CapabilityDiscovery`, escolhe um único candidato e encaminha uma `ActionRequest` ao `ActionGateway`. Ele não redefine estratégia, não autoriza e não executa comandos diretamente.

## Fase 13 — Workflow/DAG

O `WorkflowEngine` existente foi evoluído, sem criar outro motor, para manter DAG/dependências, retry, lease/fencing e idempotência, e acrescentar paralelismo limitado por `maxParallelism`, cancelamento cooperativo, timeout por node, status `CANCELLED`/`TIMED_OUT`, evidências intermediárias e persistência dessas evidências. A assinatura antiga com execute como trailing lambda foi preservada.

## Fase 14 — Job Store

Foi criado `com.brain.job.JobStore` como persistência durável independente do lease do workflow. Ele registra `CREATED`, `QUEUED`, `RUNNING`, `WAITING`, `SUCCEEDED`, `FAILED`, `CANCELLED` e `RETRYING`, aplica transições válidas, mantém tentativas, payload, evidências e erro, e recupera o estado de arquivo após reinício.
