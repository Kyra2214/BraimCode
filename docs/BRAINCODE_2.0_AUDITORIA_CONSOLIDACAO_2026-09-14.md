# Auditoria de consolidação BrainCode 2.0

**Data:** 2026-09-14  
**Escopo:** auditoria do HEAD inicial e implementação incremental das 15 fases do plano mestre.

## Resultado da auditoria

O BrainCode atual possui duas implementações complementares, não duas autoridades equivalentes:

| Área | Componente existente | Estado encontrado | Decisão de consolidação |
|---|---|---|---|
| Catálogo de APIs/modelos | `com.brain.router.ApiCatalog`, `InMemoryApiCatalog`, `DynamicFreeApiCatalog` | Catálogo operacional de provider/modelo, métricas, refresh e fallback | Preservar como catálogo operacional; não transformá-lo em registry universal |
| Registro global de capabilities | Não havia um modelo único para API, provider, agent, skill, tool, command, sandbox e workflow | Função ausente | Criar `CapabilityDefinition` e `CapabilityRegistry` |
| Discovery de APIs | `ApiDiscoveryEngine` | Normaliza candidatos externos de modelos e exige proveniência/confiança | Preservar; publicar/adaptar metadados no registry universal |
| Router/provider | `AIRouter`, `DefaultAIRouter`, `ProviderDispatcher` | Seleciona e executa modelos com métricas de qualidade, confiabilidade, velocidade e falha | Preservar; o router não vira autoridade do catálogo universal |
| Policy | `PolicyBroker`, `PolicyContext`, `ExecutionAuthorization` | Autorização deny-by-default, token e limites de sandbox | Evoluir o existente; registry não autoriza execução |
| Skills | `SkillRegistry`, `SkillManifest` | Registro especializado com assinatura, confiança e revogação | Preservar e publicar metadados no registry universal |
| Sandbox | `CapabilityResolver`, `AgentSandboxSession`, `SandboxExecutor` | Allowlist de capacidades e tradução segura para execução | Preservar; resolver continua executor especializado |
| Agents | `BoundAgent`, `AgentRegistry` no módulo Android | Agentes bounded com missão, capabilities permitidas e evidência | Preservar e publicar metadados no registry universal |
| Planner/workflow | `Planner`, `PlanoExecucao`, `WorkflowEngine` | Plano/DAG, dependências, retry e lease já existentes | Evoluir; não criar `Planner2` ou `Workflow2` |
| Memory/knowledge/critic | `ExperienceMemory`, `KnowledgeLearningCycle`, `ConservativeKnowledgeCritic` | Memória, proveniência e validação conservadora já implementadas | Consolidar no Knowledge Compiler; não promover resposta externa diretamente |

## Decisões de design

O `CapabilityDefinition` é a representação declarativa comum. Ele contém identidade, descrição, categoria, owner/origem, parâmetros, dependências, permissões, risco, custo, latência estimada, confiabilidade, qualidade, suporte a arquivos/web/código/reasoning, disponibilidade, versão, status, providers e proveniência.

O `CapabilityRegistry` é o registry único de metadados. Ele oferece registro, atualização, remoção, consultas por categoria/capability/risco/custo/provider e discovery estrutural. Ele não aplica Policy, não chama rede e não escolhe executor.

O `ApiCatalogRegistry` foi preservado porque possui consumidores Android reais e representa estado operacional de modelos descobertos. Ele não é a autoridade de capabilities: sua responsabilidade permanece limitada a `ApiCatalog` e métricas de provider/modelo.

## Fase 1 — Universal Capability Model

Criado `com.brain.capability.CapabilityDefinition`, com enums de categoria, risco, custo, disponibilidade e status, parâmetros, schemas, capabilities requeridas/fornecidas e proveniência obrigatória.

## Fase 2 — Capability Registry

Criado `com.brain.capability.CapabilityRegistry`, único registry declarativo para APIs, providers, agents, skills, tools, commands, sandbox capabilities, workflows e componentes internos. Componentes especializados publicam metadados nele sem perder suas responsabilidades operacionais ou de confiança.

## Fase 3 — Capability Discovery

Consolidado `CapabilityDiscovery` sobre o registry único. A descoberta segue `intenção → categoria → candidatas estruturais → filtro de policy → ranking → limite de candidatos`. O ranking combina compatibilidade, qualidade, confiabilidade, custo, latência, disponibilidade, risco, provider preferido e contexto declarativo. Discovery não autoriza nem executa.

## Fase 4 — Policy Broker

O `PolicyBroker` existente foi evoluído para consultar o `CapabilityRegistry` quando fornecido e avaliar actor, capability, recurso, classificação de dados, ambiente, risco, sandbox, rede, filesystem, orçamento, TTL e aprovação. `PolicyOutcome` expõe `ALLOW`, `DENY`, `REQUIRE_APPROVAL` e `SANDBOX_ONLY`; `Decision.ASK` permanece como compatibilidade interna.

## Fase 5 — Action Gateway

Criado `com.brain.gateway.ActionGateway` no módulo `:brain`. Ele resolve a capability, consulta o `PolicyBroker`, valida token/recursos, não aceita comando bruto, chama somente um `ActionExecutor` injetado e registra `actionId`, actor, capability, parâmetros redigidos, decisão, início/fim, resultado, erro, evidence e provenance. Os gateways Android especializados foram preservados.

## Fase 6 — Agent Registry

O `AgentRegistry` bounded existente passou a publicar definições `CapabilityCategory.AGENT` por `publishTo`. O índice local continua armazenando objetos executáveis; a publicação é apenas declarativa. Missão, allowlist e evidência continuam obrigatórias. A validação Android ficou bloqueada no ambiente por ausência de Android SDK (`ANDROID_HOME`/`sdk.dir`).

## Fase 7 — Skill Registry

O `SkillRegistry` especializado continua responsável por manifests, assinatura, trust e revogação. Skills habilitadas são publicadas no registry universal por `publishTo`; a publicação não concede autorização.

## Fase 8 — Retrieval

Criado `com.brain.retrieval.Retrieval` com ordem fixa `MEMORY → SKILLS → PROMPT_LIBRARY → TOOLS → AGENTS → APIS`. Memory e Skills exigem hits validados; o primeiro hit válido encerra a busca e impede chamadas de fontes posteriores.

## Fase 9 — Knowledge Compiler

Criado `KnowledgeCompiler` sobre `KnowledgeLearningCycle` e `ConservativeKnowledgeCritic`. O fluxo é `KnowledgeEvidence → candidate KnowledgeEntry → Critic → validated knowledge`; resposta externa sem fonte permanece incerta. A criação de `SkillCandidate` e o registro no `SkillRegistry` exigem validação explícita.

## Fase 10 — Planner

`PlanoExecucao` foi mantido como contrato canônico e recebeu o alias `ExecutionPlan` e `requiredCapabilities`. O Planner produz objetivo, passos, dependências e critérios; não executa nem escolhe executor.

## Fase 11 — Function Splitter

A lógica do `KeywordPlanner` foi extraída para `FunctionSplitter`/`KeywordFunctionSplitter`. O Planner solicita funções declarativas e monta `ExecutionPlan`; cada função declara capability, dependências, papel, risco e critério de sucesso.

## Fase 12 — Dispatcher

Criado `com.brain.dispatch.Dispatcher`, que recebe tarefa do `ExecutionPlan`, consulta `CapabilityDiscovery`, escolhe um candidato e encaminha uma `ActionRequest` ao `ActionGateway`. Ele não redefine estratégia, não autoriza e não executa comandos diretamente.

## Fase 13 — Workflow/DAG

O `WorkflowEngine` existente foi evoluído para manter DAG/dependências, retry, lease/fencing e idempotência, e acrescentar paralelismo limitado, cancelamento cooperativo, timeout por node, status `CANCELLED`/`TIMED_OUT`, evidências intermediárias e persistência dessas evidências. A assinatura antiga foi preservada.

## Fase 14 — Job Store

Criado `com.brain.job.JobStore` como persistência durável independente do lease do workflow. Ele registra `CREATED`, `QUEUED`, `RUNNING`, `WAITING`, `SUCCEEDED`, `FAILED`, `CANCELLED` e `RETRYING`, aplica transições válidas, mantém tentativas, payload, evidências e erro e recupera estado após reinício.

## Fase 15 — Auto-Skills

Criado `AutoSkillDetector`, que só produz propostas quando há execuções repetidas, bem-sucedidas e com evidência. O detector não chama `SkillRegistry`; promoção continua dependendo de validação explícita.

## Componentes novos

- `brain/src/main/kotlin/com/brain/capability/CapabilityModel.kt`
- `brain/src/main/kotlin/com/brain/capability/CapabilityRegistry.kt`
- `brain/src/main/kotlin/com/brain/capability/CapabilityDiscovery.kt`
- `brain/src/main/kotlin/com/brain/gateway/ActionGateway.kt`
- `brain/src/main/kotlin/com/brain/retrieval/Retrieval.kt`
- `brain/src/main/kotlin/com/brain/memory/KnowledgeCompiler.kt`
- `brain/src/main/kotlin/com/brain/memory/AutoSkills.kt`
- `brain/src/main/kotlin/com/brain/dispatch/Dispatcher.kt`
- `brain/src/main/kotlin/com/brain/job/JobStore.kt`
- testes Kotlin correspondentes
- este relatório

## Componentes consolidados e preservados

Nenhum componente funcional foi removido. Foram evoluídos/adaptados `PolicyBroker`, `SkillRegistry`, `AgentRegistry`, `KeywordPlanner` e `WorkflowEngine`. Foram preservados o catálogo dinâmico de APIs, router/provider, `CapabilityResolver` do Sandbox, agentes bounded, memória, Critic e gateways Android especializados.

## Blockers restantes

O build completo (`./gradlew test`) e `./gradlew :app:assembleDebug` não puderam completar porque o ambiente não possui Android SDK configurado (`ANDROID_HOME`/`ANDROID_SDK_ROOT` vazios e `local.properties` sem `sdk.dir`). O módulo JVM `:brain` compilou e passou todos os testes. A integração Android deve ser revalidada em um ambiente com SDK instalado.

Também permanece o blocker arquitetural já documentado do produto: o chat Android ainda não deve ser declarado como caller comprovado do gateway sem uma validação de wiring real, e o isolamento OS-level do Sandbox continua fora do escopo desta consolidação.
