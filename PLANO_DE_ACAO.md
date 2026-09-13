# Plano de Ação — BrainCode

> Gerado a partir da auditoria de 2026-09-12 (ver `AUDITORIA_PESADA.md`). Este documento existe para não repetir o erro que a auditoria encontrou: marcar algo como "concluído" sem que esteja de fato ligado ao app que roda no device. Nenhum item aqui deve ser marcado `[x]` sem: implementação + teste automatizado + **uso real por quem chama** (não só pelo próprio teste).

## Diagnóstico em uma frase

O repositório contém **dois produtos que não se falam**: o Sandbox Mobile (roda de verdade — rootfs, plugins locais, 3 abas) e o Brain em Kotlin/`:brain` (biblioteca robusta e testada, mas nunca chamada pelo app). Dentro do próprio `app`, vários subsistemas (Git, Workspace, Services, TestLab, Security*, Toolchains, RemotePluginCatalog) também estão implementados e testados, mas sem nenhuma tela ou botão que os alcance.

---

## Fase A — Verdade documental (sem risco, fazer primeiro)

Objetivo: nenhum documento do repo deve alegar integração que não existe. Isso é pré-requisito pra qualquer decisão de arquitetura, porque hoje `ROADMAP_UNIFICADO.md` e `AUDITORIA_PESADA.md` estão desalinhados com o código.

- [ ] Corrigir `README.md`: contagem de testes (116 → 134), remover os 3 links quebrados (`docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md`, `docs/ROADMAP_IMPLEMENTADO.md`, `docs/IMPLEMENTATION_STATUS.md`), adicionar seção "Estado real de integração".
- [ ] Reescrever `AUDITORIA_PESADA.md`: tirar a alegação de que "Android Mobile: não iniciado" (hoje há 3 módulos Gradle reais), atualizar contagem de testes, documentar o achado central (Brain isolado do app).
- [ ] Corrigir `ROADMAP_UNIFICADO.md`: rebaixar a Fase 3 ("Ciclo Android unificado") de ✅/🟡 para refletir que a ponte existe mas não é chamada por ninguém fora de teste; mesma correção pontual nos itens que citam `BrainSandboxExecutionBridge`, `ObservableDelivery`, `SecurityScenarioCatalog`, `RemotePluginCatalog`, `ToolchainManager` como prontos.
- [ ] Atualizar `TAREFAS_PENDENTES.md` com as pendências novas listadas na Fase B abaixo.

*(Estas quatro edições já estão aplicadas neste pacote — ver arquivos atualizados.)*

---

## Fase B — Decidir o destino de cada peça órfã

Cada linha é uma decisão de produto, não técnica: **integrar** (fazer o app chamar de verdade) ou **arquivar** (remover ou mover pra um lugar que deixe claro que não faz parte do build ativo). Nada deve ficar no meio-termo atual (implementado, testado, mas solto).

| Componente | Onde está | Situação hoje | Opção A: Integrar | Opção B: Arquivar |
|---|---|---|---|---|
| `BrainSandboxExecutionBridge` + `CicloExecucaoPlano` + `BrainExecutionCoordinator` | `android-module`, `brain` | **Primeira fatia ligada à UI**: `SandboxViewModel` aciona `BrainSandboxController.healthCheck()` depois que o runtime real está pronto; execução Android completa ainda precisa de testes no ambiente com SDK | Expandir de `sandbox.health` para planos de usuário e ligar aprovações/retomada à UI | Mover pra `experimental/`, deixar claro que é protótipo |
| Todo o `:brain` (Skills, Workflows, APIs, Discovery, Memory, Events) | `brain/src/main` | Zero chamada fora do próprio módulo | Definir um primeiro caso de uso real (ex.: rodar 1 skill via UI) como prova de integração | Rebaixar no roadmap pra "biblioteca standalone, integração não iniciada" |
| `WorkspaceManager`, `GitManager`, `ServiceManager`, `TestLab` | `app/sandbox` | Instanciados pela fachada e acionados na aba Operações; fluxo básico implementado | Expandir operações Git e adicionar terminal dedicado | — |
| `SecurityTestLab`, `SecurityAssessmentEngine`, `SecurityProjectScanner` | `app/sandbox` | Instanciados pela fachada e avaliação acionável na aba Operações | Completar executor adversarial, corpus e readiness gate | — |
| `ToolchainManager`/`ToolchainDetector` | `app/sandbox` | Instanciados pela fachada e acionados na aba Operações | Adicionar SDK/NDK, rollback transacional e cache | — |
| `SecurityScenarioCatalog` | `app/sandbox` | Conectado à avaliação de segurança como catálogo baseline | Expandir cenários e evidências | — |
| `RemotePluginCatalog` | `app/sandbox` | `PluginManager` real só usa `BuiltInCatalog` | Fazer `PluginManager` consultar o catálogo remoto como fonte adicional | Arquivar até ter transporte remoto real |
| `GitOperation` (enum) | `GitManager.kt` | Não usado nem pelo próprio arquivo | — | Removido nesta rodada |
| `ObservableDelivery`, `InMemoryExperienceMemory`, `DefaultPromptGenerator`, `HttpProviderClient` | `brain` | Zero uso, zero teste | — (dependem da decisão sobre `:brain` acima) | — |
| `reference/braincode-python/` | raiz | Snapshot congelado, nada importa dele | — | Mover pra fora do repo de build ou marcar com `.buildignore`/README já existe, mas deixar isso explícito no `README.md` principal |
| `__pycache__/*.pyc` no zip | `tests/`, `brain_runtime/` | Contradiz o próprio `.gitignore` | — | Apagar antes do próximo commit/export |

**Recomendação de ordem, se for integrar em vez de arquivar:** primeiro Workspace/Git/Services/TestLab (menor esforço, backend pronto, só falta UI) → depois o caso de uso mínimo do `:brain` → só então RemotePluginCatalog e Security* completo, que dependem de infraestrutura externa (transporte remoto, executor adversarial) fora do escopo do runtime.

### Primeira fatia implementada — 2026-09-12

O botão **Verificar pelo Brain**, na tela de Validação, agora percorre o caminho real `SandboxViewModel → BrainSandboxController → BrainSandboxExecutionBridge → CicloExecucaoPlano → PolicyBroker → AgentSandboxSession → CapabilityResolver → ManagedSandboxRuntime`. O caso de uso atual é deliberadamente pequeno: executar `sandbox.health` com autorização deny-by-default e exibir o resultado aprovado ou reprovado na UI. Isso prova a ligação fora de teste, mas não representa a unificação completa: o comando livre existente, plugins, Workspace, Git, Services, TestLab, Security* e Toolchains ainda têm caminhos próprios ou não estão expostos.

Validação desta fatia: `./gradlew :brain:test --no-daemon` passou com JDK 17 e a suíte Python passou com 134 testes. A validação dos módulos Android permanece pendente neste ambiente por ausência de Android SDK configurado.

---

## Fase C — Higiene imediata (baixo risco, pode ir junto com a Fase A)

- [ ] Remover `__pycache__/` e `*.pyc` do pacote antes de qualquer commit/export novo.
- [ ] Confirmar que `config/providers|routing|research|policy|execution/` (hoje só `.gitkeep`) estão documentados como placeholders vazios, não como "configuração pronta".
- [ ] Registrar explicitamente no `README.md` que `reference/braincode-python/` é arquivo histórico, não faz parte do build (`:brain`/`brain_runtime` atual é o que vale).

---

## Fase D — Releases (sem pendência crítica, um ajuste de escopo)

- [x] As três releases RootFS (`0.3.3`, `0.4.1`, `0.5.0`) estão migradas e documentadas corretamente — nenhuma ação necessária aqui.
- [x] Documentar explicitamente que o app hoje só baixa/consome a `0.3.3` via `rootfs_manifest.json`; `0.4.1` e `0.5.0` existem como releases, mas não têm manifest nem seleção de perfil no app. A evidência está em `docs/SANDBOX_RELEASE_MIGRATION.md` e `TAREFAS_PENDENTES.md`.

### Próxima frente — homologação Android ARM64

- [ ] Validar `:android-module` e `:app` em ambiente com Android SDK configurado e executar o fluxo em emulador ou device.
- [ ] Validar RootFS/proot em dispositivo ou emulador Android real, incluindo os perfis `0.3.3`, `0.4.1` e `0.5.0`.
- [ ] Registrar evidências de health check, extração, execução de comandos, lifecycle e integridade dos artefatos.

---

## Critério de "concluído" daqui pra frente

Uma linha só vira ✅ em qualquer documento do repo quando houver:
1. Implementação.
2. Teste automatizado.
3. **Uma chamada real, fora do próprio teste, a partir do caminho que o usuário final aciona** (UI, ViewModel, ou orquestrador Python que o `README.md` ensina a rodar). Item 3 é o que faltou até agora e é o que esta auditoria adiciona ao critério já existente no `ROADMAP_UNIFICADO.md`.
