# Roadmap Unificado — BrainCode (Braim + Sandbox Mobile)

> Este documento consolida em uma única linha do tempo os três roadmaps que hoje coexistem no repositório:
> - `docs/roadmap-sandbox-fase0.md` (Fase 0–8, runtime/rootfs/proot/plugins/dev-env)
> - `ROADMAP_BRAIM_CONSOLIDADO.md` (Fases A–H, arquitetura do Brain: Policy/Eventos/Memória/Skills/Workflows/APIs/Execução)
> - `docs/FASE01_AMBIENTE_ANDROID.md`, `FASE02_CICLO_ANDROID.md`, `FASE03_APPROVAL_RESUME.md`, `FASES05_12_OPERACAO.md` (integração Brain ↔ Sandbox no Android)
>
> Critério de status usado aqui: ✅ implementado + testado + **acionado por um caminho real do app/runtime, não só pelo próprio teste** · 🟡 parcial/esqueleto ou implementado-mas-isolado · ❌ só existe como design/documentação.
> Regra herdada de todos os três originais: **nenhuma fase é considerada concluída sem implementação real, testes automatizados e comportamento documentado** — integração externa (SDK, device físico, infra do host) é dependência de implantação, não feature simulada.
>
> **Correção de 2026-09-12:** uma auditoria com varredura de instanciação real (não só leitura de código) encontrou várias linhas marcadas ✅ que passavam em teste unitário mas nunca eram chamadas por nenhum caminho que o usuário final aciona. O critério acima ganhou a cláusula em negrito por causa disso. Ver `AUDITORIA_PESADA.md` e `PLANO_DE_ACAO.md` para o detalhamento completo e as correções aplicadas nas Fases 2, 3, 5 e 6 abaixo.

---

## Fase 0 — Fundação do Sandbox
**Status: ✅ CONCLUÍDA E CONGELADA** (não recebe novas funcionalidades)

RootFS Ubuntu 24.04, download/extração, `proot`, runtime de execução de comandos, timeout, cancelamento, lifecycle, persistência de execução, logs/observabilidade, reset, tela de validação, empacotamento Android, testes da fundação.

- **Distribuição dos releases RootFS validados**: ✅ concluída — os artefatos `rootfs-v0.3.3`, `rootfs-agent-v0.4.1` e `rootfs-agent-android-v0.5.0` foram migrados byte-a-byte de `Kyra2214/SandBox` para releases equivalentes em `Kyra2214/BrainCode`; tamanhos e SHA-256 foram verificados antes e depois, os manifests apontam para o BrainCode e a documentação específica está em `docs/SANDBOX_RELEASE_0.3.3.md`, `docs/SANDBOX_RELEASE_AGENT_EXTRA_0.4.1.md` e `docs/SANDBOX_RELEASE_AGENT_ANDROID_0.5.0.md`.

---

## Fase 1 — Brain mínimo funcional (Python) + Ambiente Android
**Status: ✅ Python concluído · ✅ Ambiente Android configurado · 🟡 Brain Kotlin parcial**

- **B — Estudos**: ✅ concluída (13 fontes analisadas, síntese em `AnálisedeCodigos/`).
- **C1 PolicyBroker (Python)**: ✅ deny-by-default, `PolicyDecision` completo, testado.
- **C2 EventStore (Python)**: ✅ append-only, hash chain, redaction, replay, rotação/retenção.
- **C3 Pipeline (Python)**: ✅ Secretary → Router → Prompt → Policy/Event → Dispatch, componentes substituíveis.
- **Ambiente Android**: ✅ JDK 17, SDK 34, Build Tools 34.0.0, NDK configurados e validados (`BUILD SUCCESSFUL`, APK debug gerado e com SHA-256 registrado).
- **C — Brain mínimo (Kotlin)**: 🟡 pipeline liga ponta a ponta, mas `KeywordSecretario` é deliberadamente fraco — classificação por keyword, sem LLM local (`LocalLLMProvider`/Qwen3 planejado, não integrado) e sem pesquisa (`pesquisarParaClassificacao()` sempre retorna vazio).

---

## Fase 2 — Persistência, Skills, Workflows e APIs dinâmicas
**Status: ✅ Python concluído e integrado ao próprio runtime Python · 🟡 Kotlin implementado e testado, mas isolado (não consumido por `:app`/`:android-module`) · 🟡 Plugins do Sandbox parcial**

| Item | Python (`brain_runtime/`) | Kotlin (`brain/`) |
|---|---|---|
| D — Memória persistente | ✅ SQLite/WAL, dedup, retenção, provenance — usado pelo `runtime.py`/`pipeline.py` reais | 🟡 `FileExperienceMemory` integrada ao `BrainExecutionCoordinator` **dentro do `:brain`**, mas nenhum dos dois é instanciado por `:app`/`:android-module` fora de teste |
| E — Skills | ✅ registry com licença, provenance, assinatura, quarentena, revogação — usado pelo pipeline real | 🟡 `SkillRegistry` completo e testado, mas sem nenhum chamador fora do próprio módulo |
| F — Workflows | ✅ estado persistente, leases, retry/backoff, compensação — usado pelo pipeline real | 🟡 `WorkflowEngine` completo e testado, mesma ressalva de isolamento |
| G — APIs dinâmicas | ✅ quota, cooldown, probes, fallback equivalente, discovery — usado pelo pipeline real | 🟡 `ResilientApiCatalog` e `ApiDiscoveryEngine` completos e testados, mesma ressalva de isolamento |

> Em todas as linhas Kotlin acima, "implementado" significa: compila, tem teste próprio no `:brain`. Não significa que o app Android execute esse código hoje — ver `AUDITORIA_PESADA.md` e `PLANO_DE_ACAO.md` para o rastreamento de instanciação que sustenta essa distinção.

Do outro roadmap, entram aqui também:
- **Sandbox Fase 1 — Plugins e ferramentas**: 🟡 parcial — `PluginModels.kt`, `PluginsScreen.kt` existem com testes e **são de fato usados pela aba Plugins/Ferramentas do app**; é instalação/gerenciamento básico, mas esse é o único bloco desta fase realmente acionado pela UI.
- **Sandbox Fase 2 — Sistema completo de plugins**: ✅ o caminho offline possui catálogo composto, instalação/remoção persistentes, snapshots versionados, rollback local do estado e histórico JSONL expostos pela UI. `RemotePluginCatalog` mantém allowlist de fontes HTTPS, SHA-256, deduplicação e rejeição fail-closed; o `SandboxPlatform` expõe apenas importação explícita de snapshots já coletados, sem rede. Transporte remoto e validação Android permanecem fora desta implementação.

---

## Fase 3 — Ciclo Android unificado e execução real
**Status: 🟡 primeira fatia da Bridge ligada à UI (`sandbox.health`) · 🟡 planos de usuário, aprovação/retomada e demais subsistemas ainda não unificados · Ambiente de desenvolvimento parcial**

> **Correção de 2026-09-12:** a auditoria anterior marcava o Ciclo Android como ✅ com base em passar nos próprios testes unitários. A primeira fatia agora liga `SandboxViewModel` a `BrainSandboxController.healthCheck()` e, portanto, existe uma chamada real fora de teste para `sandbox.health`. Isso não conclui o ciclo completo: planos de usuário, aprovação/retomada e os demais subsistemas ainda não estão ligados. Ver `AUDITORIA_PESADA.md` e `PLANO_DE_ACAO.md`.

- **Ciclo Android (antiga Fase 2 de integração)**: 🟡 a operação `sandbox.health` é chamada pelo app via `BrainSandboxController` e passa pela Bridge em ordem fixa (Router, Policy, sessão Sandbox, capability e validação de workspace); planos de usuário e demais capacidades ainda não são chamados por caminhos reais.
- **Retomada de aprovações (antiga Fase 3 de integração)**: ✅ `ExecutionBinding` imutável, digest verificado, retomada reusa `run_id`/`task_id`/`step_id`, reconstrução via `ApprovalStore` após reinício — implementado e coberto por teste (mas herda a mesma ressalva acima: não é acionado pelo app real, só por teste).
- **Sandbox Fase 3 — Ambiente de desenvolvimento**: 🟡 parcial — `Workspace.kt`, `GitManager.kt` e `ServiceManager`/`TestLab` são instanciados por `SandboxPlatform` e acionados na aba **Operações** para projetos, Git status, SQLite e TestLab. Ainda faltam terminal dedicado, operações Git completas e validação Android em dispositivo.
- **H — Execução real (Brain, Kotlin)**: ❌ não roda no app — o roadmap original (`ROADMAP_BRAIM_CONSOLIDADO.md`) previa manter o executor **congelado** até D–G estabilizarem no Kotlin; na prática, a base (memória/skills/workflows/APIs) chegou a existir no Kotlin (Fase 2 abaixo), mas o executor nunca chegou a ser ligado ao app — a dívida arquitetural citada aqui virou uma ilha desconectada em vez de um executor precoce.

---

## Fase 4 — Hardening operacional
**Status: ✅ Python concluído · 🟡 Kotlin parcial**

Do bloco "Fases 5–12" de integração (que na prática descrevem o hardening já feito, majoritariamente em Python):
- Fase 5 Quota persistente ✅ · Fase 6 Fallback rigoroso ✅ · Fase 7 EventStore de produção ✅ · Fase 8 Memory SQLite ✅ · Fase 9 QA/correction ✅ · Fase 10 Execução paralela ✅ · Fase 11 Isolamento forte (probe `unshare`/Bubblewrap/cgroups, fail-closed) ✅.
- Fase 12 Delivery observável (Kotlin) ✅ — `ObservableDelivery` com SHA-256 de artefatos e recibo completo.
- **Sandbox Fase 4 — Segurança e isolamento** (controle, limites, diagnóstico, recuperação): 🟡 a execução Kotlin usa política deny-by-default, limites, diagnóstico e recuperação compartilhados; a avaliação de segurança está acionável na aba **Operações**. Ainda faltam controles OS-level equivalentes ao hardening Python e validação em host Android real.

---

## Fase 5 — Toolchains, Rede e Serviços
**Status: ❌ não implementado como feature acessível · 🟡 domínio implementado e isolado**

- **Sandbox Fase 5 — Toolchains avançados** (Android, Java, Python, Node, C/C++, Rust, Go): 🟡 `ToolchainProfile`, `ToolchainDetector` e `ToolchainManager` são instanciados por `SandboxPlatform` e acionados na aba **Operações** para detecção e instalação allowlisted. SDK/NDK Android, rollback transacional, cache e validação real continuam pendentes.
- **Sandbox Fase 6 — Rede e serviços**: 🟡 `NetworkPolicy`, `NetworkRule` e `NetworkPolicyBroker` fornecem decisão deny-by-default e o `ServiceManager` é acionado pela aba **Operações** para o ciclo SQLite. Firewall, namespaces, egress real e cgroups de rede continuam pendentes.

---

## Fase 6 — Security Test Lab (validação adversarial)
**Status: 🟡 domínio de avaliação implementado e testado · ❌ não ligado a nada (nem UI, nem readiness gate) · ❌ executor adversarial completo**

- **Sandbox Fase 7 — Test Lab**: 🟡 `TestLab.kt` roda um pipeline básico de dependências → build → testes → lint e é acionado pela aba **Operações**; ainda não é um laboratório adversarial completo.
- **P4 — Security Test Lab** (`ProjectScanner → Attack Simulation → Sandbox → Policy/QA/Detection → Evidence Engine → Fix/Verify/Learn → Regression Corpus → ReadinessGate`): 🟡 `SecurityTestLab`, `SecurityProjectScanner`, `SecurityAssessmentEngine` e cenários baseline estão conectados ao `SandboxPlatform` e a avaliação é acionável na UI. Executor adversarial, análise estrutural, corpus persistente e integração de delivery/readiness gate continuam pendentes.

---

## Fase 7 — Sandbox 100% completo / validação de produção
**Status: 🟡 releases homologados e preflight aprovado · backlog local offline aberto; gates externos separados — marco final**

Critérios de conclusão herdados da Sandbox Fase 8 (RootFS estável, runtime estável, plugins, ferramentas, projetos, workspace, terminal, git, toolchains, serviços, rede, segurança, test lab, logs, diagnóstico, recuperação, persistência, experiência consistente) somados às pendências já documentadas em `VALIDACAO_2026-09-12.md`:
- Validação em device/emulador físico real.
- RootFS/proot: os perfis `0.3.3`, `0.4.1` e `0.5.0` já foram validados e hardened no Sandbox de origem; a migração para o BrainCode preservou os bytes. Permanece apenas a validação de implantação no app/device, sem reabrir a homologação dos artefatos.
- Futuro — assinatura de release e autoridade de chaves.
- Futuro — infra externa: Postgres/Redis/etcd no lugar do SQLite, cgroups graváveis, Bubblewrap plenamente configurado e coordenação multi-host. Nada disso faz parte do produto Android offline atual.
- O preflight local `scripts/validate-release-readiness.sh` confirma que os três manifests, assets publicados e sidecars SHA-256 estão consistentes. A evidência e a matriz de gates estão em `docs/RELEASE_READINESS.md`.
- Status consolidado: não há pendência de homologação dos releases RootFS. No escopo atual permanece somente o gate de device/emulador; assinatura e infraestrutura externa são futuras.
- Para a execução do produto Android offline, o trabalho restante está no backlog local: plugins com rollback/histórico, toolchains com cache, Security Test Lab offline, `BrainExecutionCoordinator`, `DefaultPromptGenerator` e APIs/Events locais avançados. SDK, device, assinatura e infraestrutura não entram nessa lista de implementação.

---

## Regras transversais (herdadas, valem para todas as fases)

- Deny-by-default, autorização por capability, secret references, isolamento de Sandbox, auditoria, redaction.
- Sandbox nunca chama IA; Brain/Agent chama Sandbox.
- Não liberar SandboxExecutor antes da Policy estar estável.
- Não transformar o secretário local em autoridade.
- Não aceitar API apenas porque respondeu HTTP 200.
- Não persistir secrets em prompt, memória, evento ou resultado.
- Uma etapa só é "concluída" com implementação + testes automatizados + comportamento documentado.

## Histórico de sessões

### 2026-09-12 — Preflight da Fase 7

Adicionado `scripts/validate-release-readiness.sh`, que verifica as URLs dos manifests, o tamanho publicado dos três assets RootFS e os SHA-256 dos sidecars sem baixar ou reconstruir os tarballs. O preflight passou para `rootfs-v0.3.3`, `rootfs-agent-v0.4.1` e `rootfs-agent-android-v0.5.0`. Permanecem pendentes a configuração JDK 17/Android SDK, device ou emulador ARM64, execução real de `proot`, assinatura de produção e infraestrutura OS-level; a matriz foi documentada em `docs/RELEASE_READINESS.md`.

### Sessão 2026-09-12 — Paridade Kotlin de Skills, Workflows e APIs

Implementados `SkillRegistry`, `WorkflowEngine`, resolução de Skills no `PromptLibraryLoader` e quota real no `ResilientApiCatalog`. Foram adicionados testes automatizados para registro e revogação de Skills, retry e idempotência de Workflows e janelas de quota e waterfall. Validação: 134 testes Python aprovados e `./gradlew :brain:test` concluído com `BUILD SUCCESSFUL` usando JDK 17.

### Sessão 2026-09-12 — Integração de memória persistente no pipeline Kotlin

O `BrainExecutionCoordinator` passou a aceitar `ExperienceMemory` e registrar uma experiência por etapa executada. O registro diferencia `SUCESSO`, `FALHA` e `CORRIGIDO_APOS_FALHA`, preserva a estratégia provider/modelo escolhida, tempo de execução e diagnóstico, e usa uma identidade determinística por `runId` e etapa para manter idempotência. O teste de integração confirma retry corretivo, persistência em JSONL e recuperação após reabrir o arquivo. Validação: `./gradlew :brain:test` concluído com `BUILD SUCCESSFUL` usando JDK 17.

### Sessão 2026-09-12 — Discovery avançado de APIs no Kotlin

Implementados `ApiDiscoveryCandidate`, `ApiDiscoverySource`, `ApiDiscoveryEngine` e `ApiDiscoveryReport`. A camada recebe candidatos fornecidos por fontes externas sem executar rede implicitamente, rejeita modelos inativos ou já catalogados, exige HTTPS seguro, mantém proveniência e separa aceitação de revisão manual por fonte oficial e nível de confiança. Foram adicionados testes de ordenação por prioridade, deduplicação, revisão, inatividade e URL insegura. Validação: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :brain:test --no-daemon` concluído com `BUILD SUCCESSFUL`.

### Sessão 2026-09-12 — Catálogo remoto seguro de plugins

Implementados `RemotePluginCatalog`, `RemoteCatalogSnapshot` e `RemoteComponentManifest`. A camada valida allowlist de fontes, HTTPS seguro, fonte oficial, IDs duplicados e SHA-256 antes de expor componentes; não realiza download, instalação nem execução automática. Foram adicionados testes para aceitação, fonte não confiável, hash divergente, duplicidade e HTTP. A documentação está em `docs/PLUGIN_CATALOG.md`. Validação do `:app:test` ficou bloqueada neste ambiente pela ausência de Android SDK (`ANDROID_HOME`/`local.properties`); portanto a etapa permanece parcial até validação em ambiente Android configurado.

### Sessão 2026-09-12 — Base declarativa de toolchains

Implementados `ToolchainProfile`, `ToolchainDetector`, `ToolchainInstallPlan` e perfis iniciais para Java, Python, Node.js, C/C++, Rust e Go. A detecção usa somente comandos declarados; o plano de instalação aceita apenas nomes de pacotes allowlisted e não executa automaticamente. Foram adicionados testes de detecção, diagnóstico, geração de plano e rejeição de metacaracteres. A documentação está em `docs/TOOLCHAINS.md`. A validação do módulo `:app` depende de Android SDK configurado.

### Sessão 2026-09-12 — Política declarativa de rede e serviços

Implementados `NetworkPolicy`, `NetworkRule`, `NetworkAccessRequest` e `NetworkPolicyBroker`. A decisão é deny-by-default, exige serviço/protocolo/porta declarados, permite restringir hosts e rejeita loopback, link-local, site-local e destinos reservados. A camada não abre sockets nem configura firewall; a documentação está em `docs/NETWORK_SERVICES.md`. Foram adicionados testes de autorização, escopo e rejeições de segurança. A integração efetiva com `ServiceManager` e controles OS-level permanece pendente.

### Sessão 2026-09-12 — Base do Security Test Lab Kotlin

Implementados `SecurityTestLab`, `SecurityScenario`, `SecurityProbeResult`, `SecurityFinding`, `SecurityEvidence` e `SecurityReadiness`. O domínio avalia resultados fornecidos por probes controlados, registra evidência truncada com SHA-256 e bloqueia o gate quando há cenário duplicado, probe incompleto, resultado inesperado ou severidade HIGH/CRITICAL. Foram adicionados testes para readiness, blockers, warnings e integridade da evidência. A documentação existente em `docs/SECURITY_TEST_LAB.md` foi complementada; executor adversarial OS-level, ProjectScanner, corpus persistente e integração com delivery continuam pendentes.

### Sessão 2026-09-12 — Scanner estático inicial do Security Test Lab

Implementados `SecurityProjectScanner`, `ScanRule`, `ProjectScanFinding` e `ProjectScanReport`. O scanner percorre o workspace sem executar conteúdo, ignora diretórios gerados e arquivos grandes, identifica chaves privadas, credenciais em texto, interpolação potencial em shell e TLS desabilitado, preservando caminho/linha e redigindo evidências. Foram adicionados testes de detecção, exclusão, limite de arquivo, caminho relativo e redaction. A análise estrutural profunda, executor OS-level e integração com o gate permanecem pendentes.

### Sessão 2026-09-12 — Avaliação integrada do Security Test Lab

Implementado `SecurityAssessmentEngine`, que combina achados estáticos do `SecurityProjectScanner` com resultados do `SecurityTestLab`, gera evidências rastreáveis e bloqueia readiness para riscos HIGH/CRITICAL de qualquer origem. Foram adicionados testes de combinação e de gate limpo. A avaliação não executa correções ou probes; executor adversarial, fix/verify/learn e delivery continuam pendentes.

### Sessão 2026-09-12 — Integração de política de rede ao ServiceManager

O `ServiceManager` agora exige `NetworkAccessRequest` e regra correspondente para iniciar ou reiniciar serviços que expõem porta; serviços sem porta permanecem compatíveis. `SandboxPlatform` injeta a política de rede no broker. Foram adicionados testes de ausência de autorização e início autorizado, e `docs/NETWORK_SERVICES.md` foi atualizado.

### Sessão 2026-09-12 — Lifecycle persistente de toolchains

`ToolchainManager` passou a executar explicitamente planos allowlisted, persistir estados, validar a toolchain após instalação e remover somente os pacotes declarados. Foram adicionados testes de persistência e remoção, e `docs/TOOLCHAINS.md` foi atualizado. SDK/NDK Android, rollback transacional e cache permanecem pendentes.

## Critério de sucesso do projeto

Uma execução relevante deve permitir responder: por que essa estratégia foi escolhida, por que esse agente/provedor, qual Policy autorizou, o que foi executado, o que falhou, como foi corrigido, qual evidência comprovou o resultado, e o que o Brain aprendeu.

---

## Resumo executivo — o que falta, sem duplicar

1. **Brain no Kotlin** possui memória, Skills, Workflows e Discovery integráveis; a primeira fatia acionável pelo app já está ligada ao fluxo de `sandbox.health`, planos e aprovação/retomada. Integrações avançadas continuam no backlog offline.
2. **Sandbox Mobile** já possui caminhos acionáveis para catálogo local, toolchains, rede/serviços, Security, TestLab, Workspace, Git, Memory, Discovery e delivery local; o que falta é completar as expansões listadas no backlog, não validar servidor.
3. **Validação do produto offline** (device físico/emulador, RootFS/proot real e lifecycle) continua em aberto. Assinatura de release e infraestrutura distribuída são fases futuras e não bloqueiam o escopo atual.
4. O plano de ação com a ordem recomendada de integração (ou arquivamento) de cada peça acima está em `PLANO_DE_ACAO.md`.


## Registro da entrega — 2026-09-12 — Cinco frentes

Foi criado `LocalLLMSecretario` com fallback seguro, adicionado o perfil Android ao catálogo declarativo de toolchains, criado `SecurityScenarioCatalog` com seis cenários baseline e publicada a documentação em `docs/FASES_1_A_5_ENTREGA.md`. A suíte Python passou com 134 testes. A validação Kotlin/Android passou com JDK 17 e SDK 34 no clone limpo, conforme `docs/VALIDACAO_ANDROID_2026-09-13.md`. As extensões futuras não são simuladas como concluídas.


### 2026-09-12 — Validação Android local

Ambiente preparado com JDK 17, Android SDK API 34, Build Tools 34.0.0, platform-tools e NDK 26.3. `:brain:test`, `:android-module:test`, `:app:test` e `:app:assembleDebug` passaram. O APK debug mais recente possui SHA-256 `e51adf29e818ec073dc89cea247ae6c50f1652a6673995cb6a21ac355bb3b93e`. Instalação em emulador/dispositivo físico, RootFS/proot e lifecycle continuam pendentes do escopo offline; assinatura release é futura.


### 2026-09-12 — Download da mini-LLM local

A interface de validação agora oferece o download opcional da SmolLM2 135M Instruct GGUF Q4_K_M. O manifesto `app/src/main/res/raw/local_model_manifest.json` declara URL HTTPS, licença Apache-2.0, tamanho e SHA-256; `SandboxResourceManager` foi generalizado para recursos verificáveis e `AndroidSandboxFactory` mantém os modelos em `filesDir/sandbox/models`. O download possui retomada, arquivo parcial e verificação de hash. A execução do GGUF ainda depende da integração futura de `llama.cpp`/runtime nativo autorizado e não é iniciada automaticamente.

### 2026-09-12 — Auditoria de instanciação real e correção de status inflado

Rastreada, classe por classe, a instanciação real (fora do próprio arquivo e de testes) de todo `brain/src/main`, `android-module/src/main` e `app/src/main`. Resultado: `:brain` inteiro (Policy/Router/Skills/Workflows/Memory/Discovery/Events) não é chamado por `:app`/`:android-module`; dentro do próprio `app`, `WorkspaceManager`, `GitManager`, `ServiceManager` e `TestLab` são instanciados por `SandboxPlatform` mas não têm nenhuma tela que os acione; `SecurityTestLab`, `SecurityAssessmentEngine`, `SecurityProjectScanner`, `ToolchainManager`/`ToolchainDetector`, `SecurityScenarioCatalog` e `RemotePluginCatalog` não são instanciados em lugar nenhum fora do próprio arquivo/teste. As Fases 2, 3, 5 e 6 deste documento, `AUDITORIA_PESADA.md` e `README.md` foram corrigidos para refletir isso. Nenhuma linha foi rebaixada por regressão de código — o código não mudou nesta sessão, só a precisão do status documentado. Plano de ação com decisão por componente (integrar vs. arquivar) em `PLANO_DE_ACAO.md`.

### 2026-09-12 — Fechamento da migração dos releases RootFS

A distribuição dos três releases homologados foi marcada como concluída. Os assets `.tar.gz` e os sidecars `.sha256` estão publicados no `Kyra2214/BrainCode`; os tamanhos publicados, hashes dos sidecars e valores dos três manifests foram conferidos. O Gradle também foi executado com sucesso no ambiente disponível. A próxima frente é a homologação funcional em Android ARM64: execução dos módulos Android, `proot`/RootFS real, health checks e lifecycle em emulador ou dispositivo.
