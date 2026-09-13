# Tarefas Pendentes — BrainCode

## Estado consolidado — 2026-09-13

A auditoria desta rodada reabriu algumas tarefas que estavam marcadas como concluídas sem uma chamada real pelo caminho do usuário. O critério agora é rígido:

> **Implementação + teste + caller real + evidência observável + documentação coerente.**

A lista abaixo é o estado técnico, não uma promessa de release.

## Concluído

### RootFS / Sandbox

- [x] Migrar `rootfs-v0.3.3` sem rebuild.
- [x] Migrar `rootfs-agent-v0.4.1` sem rebuild.
- [x] Migrar `rootfs-agent-android-v0.5.0` sem rebuild.
- [x] Preservar bytes, tamanhos, SHA-256 e sidecars.
- [x] Atualizar manifests e documentação da migração.

### Android local

- [x] Preparação/lifecycle/reset do Sandbox.
- [x] Plugins catalogados com instalação/remoção e snapshots locais.
- [x] Workspace básico.
- [x] Git status.
- [x] SQLite Service básico.
- [x] TestLab básico.
- [x] Security assessment estático/sintético.
- [x] Toolchains catalogadas com detecção/instalação.
- [x] `sandbox.health` pela ponte Brain → Policy → Capability → Sandbox.
- [x] Demo de approval/resume para `sandbox.health`.
- [x] ObservableDelivery local.

### Python / Brain

- [x] Runtime Python de referência e suíte histórica de 134 testes.
- [x] Policy/approval/eventos/sandbox/workflows/APIs/skills/memory/readiness conforme documentação do runtime.
- [x] `BrainExecutionCoordinator` implementado e testado como API do módulo `:brain`.
- [x] `DefaultPromptGenerator` implementado e exposto programaticamente pela fachada Android.
- [x] Security Regression Corpus implementado e persistente.
- [x] Toolchain transaction store/cache implementado.

## Pendências de integração — prioridade alta

### P0 — Segurança do caminho de execução

- [ ] **Eliminar ou colocar atrás de modo de manutenção explícito o terminal livre da UI.** Hoje `SandboxViewModel.runCommand()` aceita texto e executa `/bin/bash -c` diretamente no runtime. Esse caminho não passa por PolicyBroker/CapabilityResolver.
- [ ] Definir e testar a política para comandos de diagnóstico. Se o terminal livre permanecer, documentar claramente que ele não é um caminho de execução autorizado por capability.

### P1 — Fechar conexões já implementadas

- [ ] Fazer o botão **Avaliar segurança** chamar `SandboxPlatform.runSecurityRegression()` para que o corpus persistente realmente participe do fluxo UI.
- [ ] Trocar `InMemoryEventStore` do `BrainIntegrationFacade` por `FileEventStore` para que eventos locais sobrevivam ao reinício.
- [ ] Decidir se `BrainIntegrationFacade.runHealthWorkflow()` é apenas demo ou se deve executar um caminho real no Sandbox. Não marcar como execução Brain → Sandbox enquanto continuar usando lambda que retorna sucesso.
- [ ] Expor `DefaultPromptGenerator` pela UI ou remover a alegação de feature disponível.
- [ ] Definir um caso de uso real para `BrainExecutionCoordinator` antes de ligá-lo à UI. Não criar wiring artificial só para marcar a tarefa como concluída.

### P2 — Correção de semântica / qualidade

- [ ] Reclassificar o rollback de Toolchain: hoje ele restaura o estado declarado removendo os pacotes registrados; não restaura versões anteriores completas do sistema de pacotes.
- [ ] Melhorar o corpus para registrar o `observedBlocked` vindo do probe real/sintético, em vez de inferi-lo apenas pela ausência de um finding `Resultado inesperado`.
- [ ] Separar explicitamente regressão sintética de ataque adversarial real na UI e documentação.
- [ ] Adicionar testes de wiring que falhem quando um componente declarado como integrado deixa de possuir caller real.

## Validação funcional externa

Estas tarefas não são desenvolvimento de feature, mas continuam necessárias para fechar o produto Android offline:

- [ ] Instalar APK Debug em ARM64 device/emulador.
- [ ] Validar `sandbox.health` no device.
- [ ] Validar plano com approval/resume no device.
- [ ] Validar RootFS/proot real no device.
- [ ] Validar start/cancel/interrupção/recovery.
- [ ] Medir startup, memória, armazenamento e comportamento de instalação/execução de plugins/toolchains.

## Fora do backlog Android offline atual

- APK Release e assinatura de produção.
- Keystore de produção.
- Servidor/backend distribuído.
- Postgres/Redis/etcd.
- Coordenação multi-host.
- Infra OS-level de produção dependente do host.
- Transporte remoto de plugins.
- `HttpProviderClient`/providers externos.

Esses itens continuam documentados como futuro e não justificam reconstruir os RootFS homologados.

## Referências

- `AUDITORIA_PESADA.md`
- `docs/MAPA_INTEGRACAO_2026-09-13.md`
- `docs/APRESENTACAO_BRAINCODE.md`
- `PLANO_DE_ACAO.md`
- `ROADMAP_UNIFICADO.md`
