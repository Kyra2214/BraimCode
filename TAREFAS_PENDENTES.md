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
- As validações de SDK, device, assinatura e host foram removidas da lista operacional e estão registradas separadamente como gates externos em `docs/RELEASE_READINESS.md`.

- [x] Validar os RootFS/profiles `0.3.3`, `0.4.1` e `0.5.0` no Sandbox de origem; a migração preservou os artefatos hardened byte-a-byte. A execução no app/device permanece apenas como teste de implantação.
- [ ] Expandir o gerenciamento offline de plugins com versionamento de snapshots, rollback local e histórico de instalações.
- [ ] Expandir Toolchains locais com rollback transacional e cache de metadados; SDK/NDK de implantação ficam fora desta lista.
- [ ] Completar o Security Test Lab offline com attack simulation determinística, corpus local persistente e integração do readiness gate.

Essas pendências são acompanhadas em [`ROADMAP_UNIFICADO.md`](ROADMAP_UNIFICADO.md) e não devem ser usadas como justificativa para reconstruir ou substituir os RootFS 0.3.3, 0.4.1 ou 0.5.0 já homologados.

## Pendências de integração (auditoria de 2026-09-12)

Levantadas em `AUDITORIA_PESADA.md` por varredura de instanciação real; detalhamento e critério de decisão (integrar vs. arquivar) em [`PLANO_DE_ACAO.md`](PLANO_DE_ACAO.md).

- [x] Ligar `BrainSandboxExecutionBridge`/`CicloExecucaoPlano` ao `SandboxViewModel`; o caminho de health, planos e retomada agora é acionável pela UI. `BrainExecutionCoordinator` permanece como API avançada ainda não exposta.
- [x] Integrar Skills, Workflows, Memory e Discovery ao app por `BrainIntegrationFacade`, com catálogo, workflow de health, memória local e pipeline Discovery acionáveis na aba **Operações**. APIs, Events e o `BrainExecutionCoordinator` avançado permanecem para a próxima fatia.
- [x] Expor na UI `WorkspaceManager`, `GitManager` e `ServiceManager` na aba **Operações**; criação/listagem de projetos, `git status` e ciclo básico do SQLite usam o executor protegido compartilhado.
- [x] Instanciar e expor na aba **Operações** `SecurityTestLab`, `SecurityAssessmentEngine`, `SecurityProjectScanner`, `ToolchainManager`/`ToolchainDetector` e `SecurityScenarioCatalog`.
- [x] Ligar `RemotePluginCatalog` ao `PluginManager` real por catálogo composto; snapshots aceitos passam a aparecer na busca e podem ser instalados pelo mesmo fluxo protegido.
- [x] Integrar `ObservableDelivery` ao app offline; o botão **Recibo local** gera hashes e recibo dos artefatos do workspace sem rede.
- Futuro, fora do backlog offline: adicionar transporte remoto e autorização local para coletar/importar snapshots; a API atual permanece explícita e sem rede implícita.
- [ ] Expor `BrainExecutionCoordinator` para planos offline avançados de múltiplas etapas.
- [ ] Expor `DefaultPromptGenerator` pela UI para gerar prompts de tarefas e correções usando biblioteca local.
- [ ] Integrar APIs/Events locais avançados ao fluxo de Operações sem provider externo.
- [x] Remover o enum `GitOperation` não utilizado do `GitManager`.
- [x] Verificar e remover `__pycache__/*.pyc` do pacote; nenhuma ocorrência permanece.
- [x] Deixar explícito no `README.md` que `reference/braincode-python/` é histórico e não faz parte do build ativo.

## Decisão de produto — Android offline sem servidor

O caminho ativo do app deve priorizar componentes locais, persistentes e acionáveis
sem backend: Skills, Workflows, Memory em arquivo, Discovery, Workspace, Git
básico, Services, Security, Toolchains, TestLab, `ObservableDelivery` e catálogo
de plugins por snapshot explícito. O `BrainExecutionCoordinator` avançado,
`DefaultPromptGenerator` exposto e um fluxo remoto completo de plugins ficam como
implementações futuras locais ou de integração, conforme o caso de uso.
`HttpProviderClient` fica explicitamente condicionado a servidor, rede e
credenciais e não faz parte do modo offline.
