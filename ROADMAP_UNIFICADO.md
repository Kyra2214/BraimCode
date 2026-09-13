# Roadmap Unificado — BrainCode (Braim + Sandbox Mobile)

> Este documento consolida em uma única linha do tempo os três roadmaps que hoje coexistem no repositório:
> - `docs/roadmap-sandbox-fase0.md` (Fase 0–8, runtime/rootfs/proot/plugins/dev-env)
> - `ROADMAP_BRAIM_CONSOLIDADO.md` (Fases A–H, arquitetura do Brain: Policy/Eventos/Memória/Skills/Workflows/APIs/Execução)
> - `docs/FASE01_AMBIENTE_ANDROID.md`, `FASE02_CICLO_ANDROID.md`, `FASE03_APPROVAL_RESUME.md`, `FASES05_12_OPERACAO.md` (integração Brain ↔ Sandbox no Android)
>
> Critério de status usado aqui: ✅ implementado + testado · 🟡 parcial/esqueleto · ❌ só existe como design/documentação.
> Regra herdada de todos os três originais: **nenhuma fase é considerada concluída sem implementação real, testes automatizados e comportamento documentado** — integração externa (SDK, device físico, infra do host) é dependência de implantação, não feature simulada.

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
**Status: ✅ Python concluído · 🟡 Kotlin parcialmente concluído · 🟡 Plugins do Sandbox parcial**

| Item | Python (`brain_runtime/`) | Kotlin (`brain/`) |
|---|---|---|
| D — Memória persistente | ✅ SQLite/WAL, dedup, retenção, provenance | ✅ `FileExperienceMemory` persistente integrada ao `BrainExecutionCoordinator`; experiências de sucesso, falha e correção após retry são registradas e recuperáveis após reinício |
| E — Skills | ✅ registry com licença, provenance, assinatura, quarentena, revogação | ✅ `SkillRegistry` com manifesto, proveniência, confiança, hash, revogação e consulta por capability; loader de prompts resolve Skill relacionada |
| F — Workflows | ✅ estado persistente, leases, retry/backoff, compensação | ✅ `WorkflowEngine` com dependências, detecção de ciclos, autorização, retry, persistência e idempotência |
| G — APIs dinâmicas | ✅ quota, cooldown, probes, fallback equivalente, discovery | ✅ `ResilientApiCatalog` com quota por janela de minuto/dia, reservas, reconciliação, cooldown e waterfall; `ApiDiscoveryEngine` cobre normalização, proveniência, deduplicação, confiança e revisão segura |

Do outro roadmap, entram aqui também:
- **Sandbox Fase 1 — Plugins e ferramentas**: 🟡 parcial — `PluginModels.kt`, `PluginsScreen.kt` existem com testes, mas é instalação/gerenciamento básico.
- **Sandbox Fase 2 — Sistema completo de plugins**: 🟡 `RemotePluginCatalog` implementa catálogo remoto fornecido pelo chamador, allowlist de fontes HTTPS, SHA-256, deduplicação e rejeição fail-closed; transporte remoto e integração final com o `PluginManager` ainda dependem de implantação e autorização local.

---

## Fase 3 — Ciclo Android unificado e execução real
**Status: 🟡 Bridge funciona · Ambiente de desenvolvimento parcial**

- **Ciclo Android (antiga Fase 2 de integração)**: ✅ `BrainSandboxExecutionBridge` como entrada única — Router, Policy, sessão Sandbox, capability e validação de workspace em ordem fixa, sem executor alternativo.
- **Retomada de aprovações (antiga Fase 3 de integração)**: ✅ `ExecutionBinding` imutável, digest verificado, retomada reusa `run_id`/`task_id`/`step_id`, reconstrução via `ApprovalStore` após reinício.
- **Sandbox Fase 3 — Ambiente de desenvolvimento**: 🟡 parcial — `Workspace.kt` (63 linhas) e `GitManager.kt` (19 linhas) existem mas são enxutos; não há um módulo de Terminal dedicado (uso é via runtime do Sandbox diretamente).
- **H — Execução real (Brain, Kotlin)**: 🟡 tecnicamente já roda (via bridge acima), mas o roadmap original (`ROADMAP_BRAIM_CONSOLIDADO.md`) previa manter o executor **congelado** até D–G estabilizarem no Kotlin — ou seja, existe uma dívida arquitetural aqui: a execução avançou antes da base (memória/skills/workflows/APIs) amadurecer no lado Kotlin.

---

## Fase 4 — Hardening operacional
**Status: ✅ Python concluído · 🟡 Kotlin parcial**

Do bloco "Fases 5–12" de integração (que na prática descrevem o hardening já feito, majoritariamente em Python):
- Fase 5 Quota persistente ✅ · Fase 6 Fallback rigoroso ✅ · Fase 7 EventStore de produção ✅ · Fase 8 Memory SQLite ✅ · Fase 9 QA/correction ✅ · Fase 10 Execução paralela ✅ · Fase 11 Isolamento forte (probe `unshare`/Bubblewrap/cgroups, fail-closed) ✅.
- Fase 12 Delivery observável (Kotlin) ✅ — `ObservableDelivery` com SHA-256 de artefatos e recibo completo.
- **Sandbox Fase 4 — Segurança e isolamento** (controle, limites, diagnóstico, recuperação): 🟡 o hardening pesado está no Python; não há módulo Kotlin equivalente dedicado no lado Sandbox Mobile.

---

## Fase 5 — Toolchains, Rede e Serviços
**Status: ❌ não implementado**

- **Sandbox Fase 5 — Toolchains avançados** (Android, Java, Python, Node, C/C++, Rust, Go): 🟡 `ToolchainProfile`, `ToolchainDetector` e `ToolchainManager` cobrem detecção, instalação explícita, validação, persistência e remoção allowlisted para Java, Python, Node, C/C++, Rust e Go; SDK/NDK Android, rollback transacional e cache ainda pendentes.
- **Sandbox Fase 6 — Rede e serviços**: 🟡 `NetworkPolicy`, `NetworkRule` e `NetworkPolicyBroker` fornecem decisão deny-by-default e o `ServiceManager` agora exige request/regra para serviços com porta; firewall, namespaces, egress real e cgroups de rede ainda pendentes.

---

## Fase 6 — Security Test Lab (validação adversarial)
**Status: 🟡 domínio de avaliação implementado · ❌ executor adversarial completo**

- **Sandbox Fase 7 — Test Lab**: 🟡 `TestLab.kt` (31 linhas) já roda um pipeline básico de dependências → build → testes → lint, mas isso é um test runner, não um "Test Lab" completo.
- **P4 — Security Test Lab** (`ProjectScanner → Attack Simulation → Sandbox → Policy/QA/Detection → Evidence Engine → Fix/Verify/Learn → Regression Corpus → ReadinessGate`): 🟡 `SecurityTestLab` avalia cenários/probes, `SecurityProjectScanner` faz análise lexical read-only com redaction e `SecurityAssessmentEngine` combina ambos no readiness gate; executor adversarial, análise estrutural, corpus persistente e integração de delivery ainda pendentes.

---

## Fase 7 — Sandbox 100% completo / validação de produção
**Status: ❌ pendente — marco final**

Critérios de conclusão herdados da Sandbox Fase 8 (RootFS estável, runtime estável, plugins, ferramentas, projetos, workspace, terminal, git, toolchains, serviços, rede, segurança, test lab, logs, diagnóstico, recuperação, persistência, experiência consistente) somados às pendências já documentadas em `VALIDACAO_2026-09-12.md`:
- Validação em device/emulador físico real.
- RootFS/proot rodando de fato em produção (hoje validado só em build/testes automatizados).
- Assinatura de release (autoridade de chaves).
- Infra externa: Postgres/Redis/etcd no lugar do SQLite, cgroups graváveis, Bubblewrap plenamente configurado, coordenação multi-host — tudo isso é dependência de implantação, não é simulado pelo runtime.

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

1. **Brain no Kotlin** já possui memória persistente integrada ao coordenador, catálogo de Skills, engine de Workflows e discovery avançado de APIs; a integração com fontes de transporte reais permanece dependência de implantação.
2. **Sandbox Mobile** ainda não tem: catálogo remoto de plugins (Fase 2), gerenciamento de toolchains (Fase 5), rede/serviços (Fase 6), e um Security Test Lab de verdade com attack simulation (Fase 6 deste documento / P4).
3. **Validação final em produção** (device físico, RootFS real, assinatura de release, infra distribuída) continua em aberto — é o gate para chamar o projeto de "Fase 8 / 100% completo".


## Registro da entrega — 2026-09-12 — Cinco frentes

Foi criado `LocalLLMSecretario` com fallback seguro, adicionado o perfil Android ao catálogo declarativo de toolchains, criado `SecurityScenarioCatalog` com seis cenários baseline e publicada a documentação em `docs/FASES_1_A_5_ENTREGA.md`. A suíte Python passou com 134 testes. A validação Kotlin permanece bloqueada neste ambiente pela ausência do JDK 17 exigido pelo Gradle; a validação Android depende de SDK configurado. As dependências externas não são simuladas como concluídas.


### 2026-09-12 — Validação Android local

Ambiente preparado com JDK 17, Android SDK API 34, Build Tools 34.0.0 e platform-tools. `:app:testDebugUnitTest` e `:app:assembleDebug` passaram. A rodada corrigiu compatibilidade de leitura de arquivos no `SecurityProjectScanner`, detecção case-insensitive de chave privada, contrato de exceção do `ServiceManager` e o relatório do script de ambiente. APK debug gerado com SHA-256 `d4bcfbc1a961218e0115f70e2080ea0c8d5ec6a77add147aa6888e5f34eb0247`. Instalação em emulador/dispositivo físico, RootFS/proot e assinatura release continuam pendentes.


### 2026-09-12 — Download da mini-LLM local

A interface de validação agora oferece o download opcional da SmolLM2 135M Instruct GGUF Q4_K_M. O manifesto `app/src/main/res/raw/local_model_manifest.json` declara URL HTTPS, licença Apache-2.0, tamanho e SHA-256; `SandboxResourceManager` foi generalizado para recursos verificáveis e `AndroidSandboxFactory` mantém os modelos em `filesDir/sandbox/models`. O download possui retomada, arquivo parcial e verificação de hash. A execução do GGUF ainda depende da integração futura de `llama.cpp`/runtime nativo autorizado e não é iniciada automaticamente.
