# Tarefas Pendentes — BrainCode

## Estado consolidado — 2026-09-13

Critério rígido:

> **Implementação + teste + caller real + evidência observável + documentação coerente.**

## Fechado nesta rodada

- [x] `BrainIntegrationFacade` usa `FileEventStore`; eventos locais persistem em `events.jsonl`.
- [x] `SandboxPlatform.runSecurityRegression()` registra os probes observados explicitamente no corpus persistente.
- [x] `SecurityRegressionCorpus` deixou de inferir `observedBlocked` por ausência de finding; recebe o resultado observado.
- [x] Teste de regressão do contrato corpus/probe adicionado.
- [x] Workflow da fachada explicitamente classificado como **workflow local**, sem alegar execução Sandbox.
- [x] `BrainExecutionCoordinator` mantido como API interna do módulo `:brain`; não haverá wiring artificial só para satisfazer a UI.
- [x] `DefaultPromptGenerator` mantido como API programática até existir uma tela/ação de prompt que tenha caso de uso real.
- [x] Security regression explicitamente documentada como sintética/determinística, não ataque adversarial real.

## Ainda pendente de implementação

### P0 — Segurança

- [ ] **Fechar o terminal livre da UI** ou colocá-lo atrás de um modo de manutenção explicitamente separado do caminho autorizado. Hoje `SandboxViewModel.runCommand()` ainda aceita texto livre e usa `/bin/bash -c`.
- [ ] Criar teste de regressão que garanta que o caminho de execução de produto não aceite shell arbitrário.

### P1 — Integração de produto

- [ ] Conectar o botão **Avaliar segurança** ao `SandboxPlatform.runSecurityRegression()` no `SandboxViewModel`/UI. O backend da regressão já está fechado; falta o caller da tela.
- [ ] Criar uma ação real de UI para `DefaultPromptGenerator`, ou remover sua exposição da fachada se o caso de uso não fizer parte do produto.
- [ ] Se o produto exigir workflows que executem no Sandbox, criar um executor de workflow baseado no `BrainSandboxController`. O workflow atual permanece deliberadamente local.

### P2 — Qualidade

- [ ] Reclassificar o rollback de Toolchain como rollback de pacotes declarados, ou implementar restauração real das versões anteriores.
- [ ] Adicionar testes de wiring/orphan detection para impedir regressões documentais.
- [ ] Repetir build/testes após os commits desta rodada.

## Validação funcional externa — ainda necessária

Estas são validações no aparelho/emulador, não novas features:

- [ ] Instalar APK Debug em ARM64.
- [ ] `sandbox.health` real.
- [ ] Approval/resume real.
- [ ] RootFS/proot real.
- [ ] Start/cancel/interrupção/recovery.
- [ ] Startup, memória, armazenamento e plugins/toolchains.

## Fora do produto Android offline atual

- APK Release/assinatura/keystore de produção.
- Servidor/backend distribuído.
- Postgres/Redis/etcd.
- Multi-host.
- Infra OS-level de produção dependente do host.
- Providers externos/transporte remoto.

Os RootFS homologados permanecem copy-only e não devem ser reconstruídos.

## Referências

- `AUDITORIA_PESADA.md`
- `docs/MAPA_INTEGRACAO_2026-09-13.md`
- `docs/APRESENTACAO_BRAINCODE.md`
- `PLANO_DE_ACAO.md`
- `ROADMAP_UNIFICADO.md`
