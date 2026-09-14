# Auditoria final do commit `7eb635f`

**Escopo:** comparação do commit auditado contra o plano mestre original.  
**Regra:** documentação não foi usada como evidência. Código não executado não foi classificado como PASS.  
**Commit verificado:** `7eb635f9fa197a79445195972db23455a4183c12`.  
**Estado Git:** branch `main` sincronizado com `origin/main`, working tree limpo.

## Evidências executadas

| ID | Evidência executada | Resultado |
|---|---|---|
| E1 | `./gradlew :brain:check` | PASS; 116 testes Kotlin concluídos sem falhas. |
| E2 | `./gradlew :brain:test --tests com.brain.e2e.BrainEndToEndTest` | PASS; E2E JVM concluído. |
| E3 | `python3 -m unittest discover -s tests -p 'test_*.py' -q` | PASS; 155 testes Python. |
| E4 | `bash scripts/architecture-gate.sh` | PASS. |
| E5 | `git diff --check 7eb635f^ 7eb635f` | PASS. |
| E6 | `bash -n scripts/*.sh rootfs-builder/*.sh` | PASS. |
| E7 | `./gradlew :app:assembleDebug` | FAIL de infraestrutura; Android SDK ausente (`ANDROID_HOME`/`sdk.dir`). |

## Auditoria fase a fase

| Requisito original | Arquivo/classe responsável | Teste que comprova | Evidência executada | Status |
|---|---|---|---|---|
| Fase 0: auditar Brain, Chat, Memory, Knowledge, Critic, API Gateway, Router, Sandbox, Agents, PromptLibrary, Execution, Security e RootFS; classificar EXISTE/PARCIAL/DUPLICADO/FALTA/SUBSTITUIR | `docs/BRAINCODE_2.0_AUDITORIA_CONSOLIDACAO_2026-09-14.md` | Nenhum teste executável específico | Nenhum teste da auditoria estrutural; apenas E1–E6 validam o código | **PARTIAL** |
| Fase 1: `CapabilityDefinition` | `com.brain.capability.CapabilityModel.kt` | `CapabilityRegistryTest`, `CapabilityDiscoveryTest` | E1 | **PASS** |
| Fase 1: `CapabilityProvider` | `CapabilityProvider.kt` | Nenhum teste específico do provider | Não executado isoladamente em E1/E2 | **PARTIAL** |
| Fase 1: `CapabilityRegistry` | `CapabilityRegistry.kt` | `CapabilityRegistryTest` | E1; também usado no E2E | **PASS** |
| Fase 1: `CapabilityCandidate` | `CapabilityDiscovery.kt` | `CapabilityDiscoveryTest`, `DispatcherTest` | E1; E2 | **PASS** |
| Fase 1: `CapabilityDiscovery` | `CapabilityDiscovery.kt` | `CapabilityDiscoveryTest`, `DispatcherTest`, E2E | E1; E2 | **PASS** |
| Fase 1: pergunta fundamental “quem executa GITHUB_READ?” | Registry/Discovery | Nenhum teste para `GITHUB_READ` nominal | Não executado | **FAIL** |
| Fase 2: `ALLOW` | `PolicyBroker.kt`, `PolicyContext.kt` | `PolicyBrokerCapabilityIntegrationTest`, `PolicyBrokerTest` | E1; E2 | **PASS** |
| Fase 2: `ALLOW_WITH_LIMITS` | `PolicyOutcome`, `PolicyDecision`, `PolicyBroker` | `PolicyBrokerCapabilityIntegrationTest` | E1 | **PASS** |
| Fase 2: `REQUIRE_APPROVAL` | `PolicyBroker` | `PolicyBrokerCapabilityIntegrationTest` | E1 | **PASS** |
| Fase 2: `DENY` | `PolicyBroker` | `PolicyBrokerCapabilityIntegrationTest` | E1 | **PASS** |
| Fase 2: escopo, dados sensíveis, capability, Sandbox e risco | `PolicyBroker`, `PolicyContext` | `PolicyBrokerCapabilityIntegrationTest`, `PolicyBrokerTest` | E1 | **PASS** |
| Fase 3: unificar Agent, Tool, API, Skill, Sandbox, Workflow e Job no Gateway | `ActionGateway.kt`, `GatewayBackedSandboxExecutor.kt`, `Dispatcher.kt`, `DurableJobRunner.kt` | `ActionGatewayTest`, `DispatcherTest`, `DurableJobRunnerTest`, E2E | E1; E2 | **PARTIAL** — o E2E usa executor de ferramenta sintético; Android real não compilou. |
| Fase 3: nenhum executor contorna Policy/Gateway | `ActionGateway`, `SandboxPlatform` | `architecture-gate.sh`, `ActionGatewayTest` | E1/E4; E2 | **PARTIAL** — o gate cobre regra arquitetural, não todos os callers Android em runtime. |
| Fase 4: migrar agents bounded para registro declarativo | `BoundAgent.kt`, `BuiltInAgentDefinitions.kt` | `AgentRegistryCapabilityTest`, `PlanGapCoverageTest` | E1 | **PARTIAL** — publicação declarativa testada; módulo Android não compilou. |
| Fase 4: `ResearchAgent` | `BuiltInAgentDefinitions.researchAgent()` | `PlanGapCoverageTest` | E1 | **PARTIAL** — definição declarativa, não execução concreta do agent. |
| Fase 4: `CodeAgent` | `BuiltInAgentDefinitions.codeAgent()` | `PlanGapCoverageTest` | E1 | **PARTIAL** — definição declarativa, não execução concreta do agent. |
| Fase 5: registry de skills reutilizáveis | `SkillRegistry.kt` | `SkillRegistryTest`, `SkillRegistryCapabilityTest` | E1 | **PASS** |
| Fase 5: candidatas `research`, `github-research`, `code-test`, `android-build`, `project-audit`, `debug` | `BuiltInSkillManifests.kt` | `BuiltInSkillManifestTest` | E1 | **PASS** como manifests candidatos |
| Fase 5: skill executada como procedimento reutilizável | `SkillRegistry`, `SkillValidator` | Nenhum teste de execução real de cada skill | Não executado | **PARTIAL** |
| Fase 6: Retrieval de Memory | `Retrieval.kt`, `RetrievalAdapters.kt` | `RetrievalTest`, E2E | E1; E2 | **PASS** |
| Fase 6: Retrieval de Knowledge | `RetrievalAdapters.knowledge` | E2E | E2 | **PASS** |
| Fase 6: Retrieval de Skill | `Retrieval.kt` | `RetrievalTest` | E1 | **PASS** |
| Fase 6: Retrieval de Prompt | `Retrieval.kt` e `PromptLibrary` existente | Nenhum E2E com hit real de PromptLibrary | Não executado | **PARTIAL** |
| Fase 6: Retrieval de Workflow | `RetrievalAdapters.workflows` | Nenhum teste específico do adapter | Não executado isoladamente | **PARTIAL** |
| Fase 6: impedir API quando há solução validada | `Retrieval.kt`, E2E | `RetrievalTest`, E2E | E1; E2 confirma que APIs não são tentadas após Knowledge | **PASS** |
| Fase 7: Evidence → Critic → Knowledge Compiler | `KnowledgeCompiler.kt` | `KnowledgeCompilerTest`, E2E | E1; E2 | **PASS** |
| Fase 7: saída Knowledge | `KnowledgeArtifacts.kt` | Nenhum teste específico de `KnowledgeArtifactKind.KNOWLEDGE` | Não executado | **PARTIAL** |
| Fase 7: saída Skill | `KnowledgeCompiler.proposeSkill` | `KnowledgeCompilerTest`, E2E | E1; E2 | **PASS** |
| Fase 7: saída Prompt | `KnowledgeArtifacts.kt` | Nenhum teste de promoção/uso | Não executado | **PARTIAL** |
| Fase 7: saída Workflow | `KnowledgeArtifacts.kt` | Nenhum teste de promoção/uso | Não executado | **PARTIAL** |
| Fase 7: saída Rule | `KnowledgeArtifacts.kt` | Nenhum teste de promoção/uso | Não executado | **PARTIAL** |
| Fase 8: `Intent` | `PlanContracts.kt`, `IntentClassifier.kt` | `PlanGapCoverageTest` | E1 | **PASS** |
| Fase 8: `ExecutionPlan` | `Planner.kt` | `ExecutionPlanContractTest`, `PlanGapCoverageTest` | E1; E2 | **PASS** |
| Fase 8: `Task` | `PlanContracts.kt`, `PlanoExecucao.tasks` | `PlanGapCoverageTest` | E1 | **PASS** |
| Fase 8: `Dependency` | `PlanContracts.kt`, `PlanoExecucao` | `PlanoExecucaoTest`, `ExecutionPlanContractTest` | E1 | **PASS** |
| Fase 8: `FunctionSplitter` | `KeywordPlanner.kt` | `FunctionSplitterTest`, `KeywordPlannerTest` | E1; E2 usa Planner | **PASS** |
| Fase 8: Planner só para decomposição necessária | `FastIntentClassifier`, `KeywordPlanner` | Nenhum teste de decisão operacional que evite Planner | Não executado como política de runtime | **PARTIAL** |
| Fase 9: Dispatcher recebe Task e encaminha ao Gateway | `Dispatcher.kt` | `DispatcherTest`, E2E | E1; E2 | **PASS** |
| Fase 10: dependências DAG | `WorkflowEngine.kt` | `WorkflowEngineTest`, `WorkflowDagFeaturesTest`, E2E | E1; E2 | **PASS** |
| Fase 10: paralelismo seguro | `WorkflowEngine.executeBatch` | `WorkflowDagFeaturesTest` | E1 | **PASS** |
| Fase 10: retry | `WorkflowEngine.executeNode` | `WorkflowEngineTest` | E1 | **PASS** |
| Fase 10: falha | `WorkflowEngine` | `WorkflowEngineTest`, `WorkflowDagFeaturesTest` | E1 | **PASS** |
| Fase 10: recuperação | `WorkflowEngine` checkpoints, `WorkflowLeaseStore` | Testes de idempotência/lease | E1 | **PARTIAL** — não houve interrupção real com retomada granular do último node. |
| Fase 10: critérios de conclusão | `WorkflowRunResult`, `WorkflowStepResult` | `WorkflowEngineTest`, E2E | E1; E2 | **PASS** no critério de sucesso/falha básico |
| Fase 11: JobStore para tarefas longas e retomáveis | `JobStore.kt`, `DurableJobRunner.kt` | `JobStoreTest`, `DurableJobRunnerTest`, E2E | E1; E2 | **PASS** no núcleo |
| Fase 12: detectar execuções recorrentes bem-sucedidas | `AutoSkills.kt` | `AutoSkillsTest` | E1 | **PASS** |
| Fase 12: sempre validar antes de `VALIDATED` | `SkillValidation.kt` | `PlanGapCoverageTest`, E2E | E1; E2 | **PASS** com validators sintéticos |
| Fase 13: descoberta por categorias | `CapabilityDiscovery.kt` | `CapabilityDiscoveryTest` | E1; E2 | **PASS** |
| Fase 13: carregamento sob demanda | `CapabilityProvider.kt`, `LazyCapabilityDiscovery` | Nenhum teste específico de `load()` | Não executado | **PARTIAL** |
| Fase 14: visualização Task → Plan → Capabilities → Policy → Agent → Sandbox → Evidence → Critic | `ExecutionTrace.kt`, ActionGateway trace | `PlanGapCoverageTest`, E2E | E1; E2 verifica Policy trace, não todos os stages no mesmo fluxo | **PARTIAL** |

## Requisitos transversais do plano original

| Requisito original | Arquivo/classe responsável | Teste que comprova | Evidência executada | Status |
|---|---|---|---|---|
| APIs devem ser capabilities, não destino direto de Task | `ApiCatalogCapabilityProvider`, `CapabilityDiscovery`, `Dispatcher` | Nenhum teste completo de API catalog → registry → Gateway | E1; E2 usa Tool, não API | **PARTIAL** |
| Provider Router deve usar catálogo dinâmico e health | `DefaultAIRouter`, `ApiCatalog`, `LiveStats` | `DefaultAIRouterTest`, `ResilientApiCatalogTest` | E1 | **PASS** para router existente; integração universal não comprovada |
| `free-first` deve preferir FREE/FREE_TIER | `ProviderModel.cost`, `DefaultAIRouter`, `RoutingWeights` | Nenhum teste que compare modelo FREE contra PAYG | Não executado | **PARTIAL** |
| LLM é capability e não autoridade absoluta | `LlmUsePolicy`, `FastIntentClassifier` | Nenhum teste de decisão contra provider LLM real | E1 compila; não há execução LLM | **PARTIAL** |
| Toda execução relevante produz Evidence | `ActionGateway`, `WorkflowEngine`, `DurableJobRunner` | `ActionGatewayTest`, `WorkflowDagFeaturesTest`, E2E | E1; E2 | **PASS** no núcleo sintético |
| Critic decide promoção de Knowledge | `ConservativeKnowledgeCritic`, `KnowledgeCompiler` | `KnowledgeCompilerTest`, E2E | E1; E2 | **PASS** |
| Sandbox é fronteira de execução | `GatewayBackedSandboxExecutor`, `SecureCommandExecutor`, `ManagedRuntimeExecutor` | Nenhum teste compilado do módulo Android | E7 falha antes da compilação Android | **PARTIAL** |
| Android → Gateway → Sandbox com execução real | `SandboxPlatform`, `GatewayBackedSandboxExecutor`, `SandboxActionExecutor` | Nenhum teste Android executado | E7: SDK ausente | **FAIL** |
| Action Lifecycle e Audit Event completos | `ActionLifecycle`, `ActionAuditRecord`, `ActionGateway` | `ActionGatewayTest`, E2E | E1; E2 | **PARTIAL** — não há execução Android real nem todos os estados transitivos. |

## Pendências reais para 100% de consolidação

1. **Disponibilizar Android SDK e executar `:app:assembleDebug`, testes Android e `android-module`**, incluindo compilação da ponte `GatewayBackedSandboxExecutor`.
2. **Executar E2E Android real** com `SandboxPlatform`, `ManagedRuntimeExecutor`, ActionGateway, Policy e `ExecutionLog`; o E2E atual é comprovadamente JVM, não Android.
3. **Implementar e executar agentes concretos `ResearchAgent` e `CodeAgent`**, além das definições declarativas.
4. **Adicionar testes executados para `CapabilityProvider`, `ApiCatalogCapabilityProvider`, `LazyCapabilityDiscovery` e o caso nominal `GITHUB_READ`.**
5. **Adicionar e executar testes de PromptLibrary e Workflow retrieval adapters**, além de uso real desses hits no Retrieval.
6. **Adicionar e executar testes para promoção/uso dos artefatos `Prompt`, `Workflow` e `Rule` produzidos pelo Knowledge Compiler.**
7. **Adicionar comparação executada de roteamento FREE/FREE_TIER contra PAYG**, usando dados de custo reais do catálogo.
8. **Executar um teste de interrupção e retomada granular de Workflow**, comprovando que nodes concluídos não são reexecutados após restart.
9. **Completar e executar a visualização integral do ExecutionTrace**, comprovando no mesmo fluxo os stages Task, Plan, Capability, Policy, Agent, Sandbox, Evidence e Critic.
10. **Comprovar por teste de integração que API, Agent, Tool, Skill, Sandbox, Workflow e Job passam pelo mesmo ActionGateway em execução real**, e não apenas por fixtures sintéticos.
