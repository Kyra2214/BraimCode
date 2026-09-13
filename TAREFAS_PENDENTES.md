# Tarefas Pendentes — BrainCode

## Atualização de status

A migração dos releases RootFS homologados foi concluída e não é mais uma pendência operacional. O backlog local offline do item 5 também foi fechado em código e documentação.

### Concluída

- [x] Migrar `rootfs-v0.3.3` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Migrar `rootfs-agent-v0.4.1` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Migrar `rootfs-agent-android-v0.5.0` do `Kyra2214/SandBox` para o release equivalente no `Kyra2214/BrainCode`.
- [x] Preservar os tarballs byte-a-byte, sem rebuild, recompressão ou alteração interna.
- [x] Copiar e validar os sidecars `.sha256` originais.
- [x] Verificar tamanho e SHA-256 antes e depois da publicação.
- [x] Atualizar os três manifests para as URLs do `Kyra2214/BrainCode`.
- [x] Publicar documentação específica de cada perfil em `docs/`.

A evidência detalhada está em [`docs/SANDBOX_RELEASE_MIGRATION.md`](docs/SANDBOX_RELEASE_MIGRATION.md).

## Backlog local offline — Item 5

- [x] Expandir Toolchains locais com rollback transacional e cache de metadados; SDK/NDK de implantação ficam fora desta lista.
- [x] Completar o Security Test Lab offline com attack simulation determinística, corpus local persistente e integração do readiness gate.
- [x] Expor `BrainExecutionCoordinator` como API avançada de coordenação de planos multi-etapas no módulo `:brain`, com Policy, aprovação, retry, eventos e memória; a fatia Android usa o controlador autorizado existente.
- [x] Expor `DefaultPromptGenerator` pela camada Android de integração para geração local de prompts de roadmap e correções com `PromptLibrary`.
- [x] Integrar APIs/Events locais avançados ao fluxo Brain/Operações sem provider externo, incluindo eventos locais, consulta por run e verificação de integridade.
- [x] Adicionar regressões determinísticas para corpus de segurança e transações/cache de toolchain.
- [x] Documentar o fechamento do item em [`docs/BACKLOG_OFFLINE_ITEM_5.md`](docs/BACKLOG_OFFLINE_ITEM_5.md).

## Unificação Brain ↔ Sandbox

- [x] Ligar uma primeira operação real da UI ao `:brain`: o botão **Verificar pelo Brain** executa `sandbox.health` através de Policy, sessão autorizada, capability resolver e runtime Sandbox.
- [x] Expandir o controlador para executar planos, exigir aprovação em passos HIGH/CRITICAL, persistir solicitações em `approvals.jsonl` e permitir consumo único via retomada na aba **Operações**.
- [x] Validar os RootFS/profiles `0.3.3`, `0.4.1` e `0.5.0` no Sandbox de origem; a migração preservou os artefatos hardened byte-a-byte.
- [x] Expandir o gerenciamento offline de plugins com snapshots versionados, rollback local do estado persistido e histórico JSONL de instalações/remoções.

As validações de SDK, device, assinatura e host foram removidas da lista operacional e estão registradas separadamente como gates externos em `docs/RELEASE_READINESS.md`.

## Fora do backlog offline

- APK Release;
- keystore e assinatura de produção;
- servidor/backend distribuído;
- Postgres/Redis/etcd;
- coordenação multi-host;
- isolamento OS-level de produção dependente do host;
- transporte remoto de plugins e `HttpProviderClient`.

Esses itens não justificam reconstruir ou substituir os RootFS 0.3.3, 0.4.1 ou 0.5.0 já homologados.
