# Tarefas Pendentes — BrainCode

## Atualização de status

A migração dos releases RootFS homologados foi concluída e não é mais uma pendência operacional.

### Concluída

- [x] Migrar `rootfs-v0.3.3` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Migrar `rootfs-agent-v0.4.1` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Migrar `rootfs-agent-android-v0.5.0` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Preservar os tarballs byte-a-byte, sem rebuild, recompressão ou alteração interna.
- [x] Copiar e validar os sidecars `.sha256` originais.
- [x] Verificar tamanho e SHA-256 antes e depois da publicação.
- [x] Atualizar os três manifests para as URLs do `Kyra2214/BrainCode`.
- [x] Publicar documentação específica de cada perfil em `docs/`.

A evidência detalhada está em [`docs/SANDBOX_RELEASE_MIGRATION.md`](docs/SANDBOX_RELEASE_MIGRATION.md), com análises por release em:

- [`docs/SANDBOX_RELEASE_0.3.3.md`](docs/SANDBOX_RELEASE_0.3.3.md)
- [`docs/SANDBOX_RELEASE_AGENT_EXTRA_0.4.1.md`](docs/SANDBOX_RELEASE_AGENT_EXTRA_0.4.1.md)
- [`docs/SANDBOX_RELEASE_AGENT_ANDROID_0.5.0.md`](docs/SANDBOX_RELEASE_AGENT_ANDROID_0.5.0.md)

## Pendências remanescentes

### Unificação Brain ↔ Sandbox

- [x] Ligar uma primeira operação real da UI ao `:brain`: o botão **Verificar pelo Brain** executa `sandbox.health` através de Policy, sessão autorizada, capability resolver e runtime Sandbox.
- [x] Expandir o controlador para executar planos, exigir aprovação em passos HIGH/CRITICAL, persistir solicitações em `approvals.jsonl` e permitir consumo único via retomada na aba **Operações**.
- [ ] Validar `:android-module` e `:app` em ambiente com Android SDK configurado e executar o fluxo em emulador ou device.

As tarefas abaixo permanecem abertas porque não fazem parte da migração copy-only dos artefatos homologados:

- [ ] Validar RootFS/proot em dispositivo ou emulador Android real.
- [ ] Realizar assinatura de release do APK com a autoridade de chaves de produção.
- [ ] Completar o catálogo remoto e o gerenciamento de plugins no Sandbox Mobile.
- [ ] Completar toolchains Android/NDK, rollback transacional e cache.
- [ ] Completar rede/serviços com controles OS-level, firewall, namespaces e egress real.
- [ ] Completar o Security Test Lab adversarial com attack simulation, corpus persistente e integração de delivery.
- [ ] Validar infraestrutura externa de produção, incluindo serviços distribuídos e cgroups/Bubblewrap configurados no host.

Essas pendências são acompanhadas em [`ROADMAP_UNIFICADO.md`](ROADMAP_UNIFICADO.md) e não devem ser usadas como justificativa para reconstruir ou substituir os RootFS 0.3.3, 0.4.1 ou 0.5.0 já homologados.

## Pendências de integração (auditoria de 2026-09-12)

Levantadas em `AUDITORIA_PESADA.md` por varredura de instanciação real; detalhamento e critério de decisão (integrar vs. arquivar) em [`PLANO_DE_ACAO.md`](PLANO_DE_ACAO.md).

- [x] Ligar `BrainSandboxExecutionBridge`/`CicloExecucaoPlano` ao `SandboxViewModel`; o caminho de health, planos e retomada agora é acionável pela UI. `BrainExecutionCoordinator` permanece como API avançada ainda não exposta.
- [x] Integrar Skills, Workflows, Memory e Discovery ao app por `BrainIntegrationFacade`, com catálogo, workflow de health, memória local e pipeline Discovery acionáveis na aba **Operações**. APIs, Events e o `BrainExecutionCoordinator` permanecem para a próxima fatia.
- [x] Expor na UI `WorkspaceManager`, `GitManager` e `ServiceManager` na aba **Operações**; criação/listagem de projetos, `git status` e ciclo básico do SQLite usam o executor protegido compartilhado.
- [x] Instanciar e expor na aba **Operações** `SecurityTestLab`, `SecurityAssessmentEngine`, `SecurityProjectScanner`, `ToolchainManager`/`ToolchainDetector` e `SecurityScenarioCatalog`.
- [x] Ligar `RemotePluginCatalog` ao `PluginManager` real por catálogo composto; snapshots aceitos passam a aparecer na busca e podem ser instalados pelo mesmo fluxo protegido.
- [ ] Adicionar transporte remoto e autorização local acionada pela UI para coletar/importar snapshots; a API atual permanece explícita e sem rede implícita.
- [x] Remover o enum `GitOperation` não utilizado do `GitManager`.
- [x] Verificar e remover `__pycache__/*.pyc` do pacote; nenhuma ocorrência permanece.
- [x] Deixar explícito no `README.md` que `reference/braincode-python/` é histórico e não faz parte do build ativo.
