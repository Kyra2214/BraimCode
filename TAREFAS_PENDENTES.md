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
- [ ] Expandir o controlador para planos de usuário e remover os caminhos de execução paralelos onde houver equivalência segura.
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

- [ ] Decidir e executar: ligar `BrainSandboxExecutionBridge`/`CicloExecucaoPlano`/`BrainExecutionCoordinator` ao `SandboxViewModel`, ou arquivar essa cadeia explicitamente.
- [ ] Decidir o destino de todo o módulo `:brain` (Skills, Workflows, APIs, Discovery, Memory, Events) — hoje zero uso fora dos próprios testes.
- [ ] Expor na UI (ou remover a instanciação) de `WorkspaceManager`, `GitManager`, `ServiceManager` e `TestLab` — hoje construídos em `SandboxPlatform` sem nenhuma tela que os acione.
- [ ] Decidir o destino de `SecurityTestLab`, `SecurityAssessmentEngine`, `SecurityProjectScanner`, `ToolchainManager`/`ToolchainDetector` e `SecurityScenarioCatalog` — nenhum é instanciado fora do próprio arquivo/teste.
- [ ] Ligar `RemotePluginCatalog` ao `PluginManager` real, ou arquivar até haver transporte remoto.
- [ ] Remover o enum `GitOperation` (não usado nem pelo próprio `GitManager`) ou passar a usá-lo.
- [ ] Remover `__pycache__/*.pyc` do pacote (contradiz o próprio `.gitignore`).
- [ ] Deixar explícito no `README.md` que `reference/braincode-python/` é histórico e não faz parte do build ativo (feito nesta rodada — conferir se persiste em futuros merges).
