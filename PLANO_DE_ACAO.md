# Plano de Ação — BrainCode

**Atualizado:** 2026-09-14.  
Base principal: `docs/AUDITORIA_30_COMMITS_2026-09-14.md`.

## Objetivo

Fechar o ciclo real do Brain sem confundir biblioteca testada com produto integrado.

## Fase 1 — Catálogo e execução de APIs

- [x] Catálogo runtime dinâmico por provider.
- [x] Política automática free-only.
- [x] Refresh antes do routing.
- [x] Substituição de modelo removido quando houver alternativa compatível.
- [x] Waterfall/fallback gratuito.
- [x] Gateway Android para requests internos.
- [x] Classificação básica de falhas HTTP.

## Fase 2 — Conhecimento

- [x] Contrato `KnowledgeMemory`.
- [x] Persistência Android.
- [x] Candidate knowledge separado de recall.
- [x] Proveniência de provider/model/URI/GitHub.
- [x] `KnowledgeLearningCycle`.
- [x] `KnowledgeCritic` automático.
- [ ] Retrieval executor usando `retrievalHints` antes de consultar API novamente.
- [ ] Deduplicação/fingerprint.
- [ ] Versionamento e histórico de correções.
- [ ] Escopos global/usuário/projeto para futura versão servidor.
- [ ] Índice escalável.

## Fase 3 — Critic semântico

- [ ] Código: Sandbox → build/test/lint → Critic.
- [ ] Fatos externos: fonte/cross-check.
- [ ] Respostas determinísticas: validação local.
- [ ] Contrato estruturado de citações/referências.

## Fase 4 — Integração Android

- [x] `sandbox.health` no caminho Brain → Policy → Capability → Sandbox.
- [ ] Comprovar caller real do `BrainApiGateway` na conversa Android.
- [ ] Integrar `BrainExecutionCoordinator` quando existir caso de uso real.
- [ ] Expor `DefaultPromptGenerator` quando houver ação de UI real.
- [ ] Fechar EventStore Android persistente no caminho principal.
- [ ] Fechar Security Regression Corpus pelo caminho principal da UI.

## Fase 5 — Segurança P0

- [ ] Isolamento de rede OS-level.
- [ ] Jail filesystem OS-level.
- [ ] Process group/session.
- [ ] Enforcement OS-level de CPU/memória/PIDs/FDs/disco.
- [ ] Trust chain autenticada dos RootFS.

## Fase 6 — Segurança P1

- [ ] Planner → ExecutionPlan → Policy → AuthorizedPlan → Agent → Sandbox por teste arquitetural.
- [ ] Credential binding completo.
- [ ] SSRF/DNS rebinding.
- [ ] EventStore rotation/hash-chain.
- [ ] Fencing de workflow leases.
- [ ] Wiring/orphan tests.

## Fase 7 — Validação do HEAD

- [ ] Suíte Python.
- [ ] `:brain`.
- [ ] `:android-module`.
- [ ] `:app`.
- [ ] Build Debug.
- [ ] Preflight release.
- [ ] Device/emulador ARM64.

**Regra:** nenhum item vira concluído somente porque existe ou passa em teste próprio. É necessário caller real e evidência quando a feature fizer parte do produto.