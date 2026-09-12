# Roadmap Braim — Consolidado GPT × Claude × Manus

> **IaBrain pensa. Agentes trabalham. SandBox executa.**

Este documento consolida a comparação das 13 análises GPT/Claude/Manus com o roadmap técnico proposto para o Braim. Ele é a referência-base antes da primeira missão de implementação.

## 1. Decisões centrais

- Manus: referência de profundidade, evidência, riscos, execução e auditoria.
- GPT: transforma descobertas em contratos, interfaces, responsabilidades e decisões arquiteturais.
- Claude: reforça modularidade, Skills, workflows, versionamento, policy gates e engenharia.
- Não copiar monorepos. Absorver padrões comprovados e validar no código.
- O Core não depende de modelo, provedor, UI ou framework externo específico.
- Sandbox nunca chama IA; Brain/Agent chama Sandbox.
- Agente não possui autoridade implícita sobre estado global.
- Skills, workflows, prompts, memória e catálogos são contexto/dados, nunca autorização.
- Execução é deny-by-default.

## 2. Arquitetura-alvo

```text
User → Session → TaskSpec → Research/Memory → Planner
     → PolicyBroker → Router → Agent/Skill/Tool
     → Sandbox → Validator/Critic → Decision
     → Pass | Correct | Retry | Fallback | Abort
     → Delivery → Memory/Learning/EventStore
```

O `PolicyBroker` e o `EventStore` são transversais e entram no P0.

## 3. Oito sessões funcionais

### S1 — Entrada
Normalizar pedido e gerar `TaskSpec` imutável:
`task_id, session_id, objective, constraints, inputs, expected_outputs, success_criteria, capabilities, priority`.

### S2 — Secretário / descoberta
Memory/Retrieval → detectar lacunas → ResearchPolicy → fontes/APIs → evidência/proveniência.
O secretário é analista, não Brain. `KeywordSecretario` deve ficar atrás de `Secretario` para futura troca por `LocalLLMSecretario`.

### S3 — Planner
`Task → Plan → Phase → Step → Capability → Agent/Tool → ExpectedResult`.
Cada Step declara dependências, entradas, saída, timeout, retry, sucesso, fallback, permissões e `parent_step_id`. Planner não executa comandos.

### S4 — Prompt/roteamento
`CapabilityRegistry → Provider/Agent Registry → RoutingPolicy → PromptBuilder → Dispatch`.
Prompt é representação para o especialista, não contrato principal. Registrar provedor/modelo, capability, versão, custo, latência, qualidade, erros, retries e fallback.

### S5 — Agentes
`ExecutionRequest → ExecutionResult` estruturado. Agentes não alteram memória global, trocam provedor, executam fora do Sandbox, persistem credenciais ou mudam plano silenciosamente.

### S6 — Sandbox
`Braim → SandboxJob → execução → testes → artefatos → SandboxJobResult → Braim`.
Contrato mínimo: `job_id, idempotency_key, session_id, commands/files, toolchain, permissions, secret_refs, timeout/cancel, stdout/stderr, exit_code, tests, changed_files, artifacts, diagnostics`.

### S7 — Validação
`Result → Validate → Critic → Decision → Pass | Correct | Retry | Fallback | Abort`.
Validar contrato/schema, completude, testes, invariantes, qualidade, evidência, segurança, timeout, custo e consistência de estado. Correção referencia `step_id` e preserva histórico.

### S8 — Entrega/aprendizado
`Problem → Source → Strategy → API/Provider → Skill → Prompt → Agent → Result → Cost → Time → Quality → Errors → Reward/Penalty`.
Separar fatos, decisões, experiências, estratégias, métricas, evidências e Skills/playbooks. Transcript bruto não é aprendizado.

## 4. Fases consolidadas

### A — Contrato Brain ↔ Sandbox
Fechar `Job`, `JobResult`, `Requisito`, `JobContext` com `risk_class`, `approval_required`, `budget`, `cancellation`, `idempotency_key`, `secret_refs`, `capabilities_required` e `artifact_manifest`.
Eventos: `JobStarted`, `JobApprovalRequested`, `JobCompleted`, `JobFailed`, `JobCancelled`.
**Executor real continua congelado.**

### B — Estudos
**Concluída.** As 13 fontes e as análises GPT/Claude/Manus estão no projeto. A síntese está em `AnálisedeCodigos/Comparacao_GPT_Claude_Manus_Sessoes_Braim.md`.

### C — Brain mínimo funcional — P0
**C1 PolicyBroker**:
`authorize(actor, capability, resource, context) → ALLOW | ASK | DENY`.
`PolicyDecision` deve conter `decisionId, runId, taskId, actor, capability, riskClass, approvalRequired, sandboxRequired, networkAllowed, filesystemRoots, budget, expiresAt, reason`.

**C2 EventStore** — P0 junto com Policy.
Append-only/replayável, com `event_id, run_id, session_id, task_id, timestamp, type, version, sequence, structured/redacted payload`.
Fluxo inicial: `TaskCreated → TaskClassified → PlanCreated → CapabilitySelected → PolicyChecked → ApprovalRequested → AgentDispatched → AgentCompleted → ValidationStarted → ValidationFailed → CorrectionRequested → Retry → ValidationPassed → Delivered`.

**C3 Pipeline**:
`Secretary → Router → Prompt → Policy/Event trace → Dispatch`.
Componentes substituíveis sem alterar o Core.

### D — Memória persistente — P1
Trocar `InMemoryExperienceMemory` por SQLite/Room + JSON para payloads grandes. Append/create-only, contexto separado de instrução, provenance, versionamento, recuperação e testes de concorrência/recovery. LEANN fica como backend semântico opcional posterior.

### E — Skills e agentes — P1
Novo `skills` com `SkillManifest {id, version, description, triggers, exclusions, body_path, resources, tools, required_permissions, trust_level}`.
Usar os 344 comandos como semente, não autorização. Primeiro especialista seguindo GEO-SEO: router → subskills → agentes → síntese → score/evidência; candidato: QA/security dogfooding.

### F — Workflows — P2
Manifesto/`WORKFLOW.md`, available/custom/enabled, custom > community, enable/disable reversível, `workflow_version`, `run_id`, `idempotency_key`, grafo de nodes, estado persistente e retry/rollback. Não importar editor/monorepo n8n/ClawFlows.

### G — APIs — P2
Evoluir `ApiCatalogEntry` com capability, schemas, custo, quota, fallback group, latência, confiabilidade, qualidade, revisão, provenance, termos/licença e health check. Waterfall não depende apenas de HTTP 500/200: considera incompletude, timeout, quota, qualidade e histórico. `DiscoveryIntelligence` alimenta catálogo com evidência.

### H — Execução real — P3
Somente após A–G estáveis: Policy + EventStore + Memory + Skills + Workflow + Router + QA + Contracts → `SandboxExecutor`.
QA vira gate obrigatório antes de aprovação/entrega. Adicionar contract tests, golden events e E2E real.

## 5. Prioridade final

```text
P0: A + C1 Policy + C2 EventStore + C3 pipeline
P1: D memória persistente + E Skills + Capability Registry + primeiro especialista
P2: F workflows + G APIs dinâmicas/waterfall/provenance/health
P3: H SandboxExecutor + QA gate + Golden Events + E2E
Depois: LEANN + OFFPack/cache + Playbooks avançados
```

## 6. Requisitos transversais

### Segurança
Deny-by-default, capability authorization, secret references, isolamento Sandbox, limites de rede/filesystem, auditoria, redaction, validação de entrada e proteção contra prompt/context injection.

### Concorrência
Estado compartilhado protegido, invariantes explícitos, idempotência, TOCTOU, cancelamento, timeout, retry limitado e recovery.

### Observabilidade
`run_id`, `session_id`, `task_id`, `step_id`, `event_id`, decisões de Policy, custo, latência, provenance, resultados redigidos e replay.

### Qualidade
Contract tests, testes comportamentais, integração, concorrência, recovery, golden events e E2E. Taxa de testes isolada não é prova suficiente.

## 7. Não fazer agora

- Não liberar SandboxExecutor antes da Policy.
- Não colocar LEANN no Core agora.
- Não transformar o secretário local em autoridade.
- Não copiar LangChain/OpenCode/n8n/ClawFlows.
- Não acoplar Core a modelo/provedor.
- Não instalar Skill de terceiro sem segurança/licença.
- Não agendar automação sem idempotência.
- Não aceitar API apenas porque respondeu HTTP 200.
- Não persistir secrets em prompt, memória, evento ou resultado.

## 8. Critério de sucesso

Uma execução relevante deve permitir responder:

1. Por que essa estratégia foi escolhida?
2. Por que esse agente/provedor foi escolhido?
3. Qual Policy autorizou a ação?
4. O que foi executado?
5. O que falhou?
6. Como foi corrigido?
7. Qual evidência comprovou o resultado?
8. O que o Brain aprendeu?

Fluxo mínimo comprovável:
`pedido → entendimento → descoberta → plano → capability/agente → Policy → execução rastreável → validação → correção/retry/fallback → entrega → memória → aprendizado`.

## 9. Antes da primeira missão

Este documento é o **roadmap-base consolidado**, não uma ordem cega de implementação.

A primeira missão do usuário deve ser analisada contra o código real, contratos existentes, as 13 análises, Policy/EventStore, segurança, concorrência, dependências e risco de retrabalho.

**Nenhuma fase deve ser implementada automaticamente apenas por este documento. Aguardando a primeira missão.**
