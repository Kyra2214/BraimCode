# Status de implementação do roadmap

A Fase B — estudos — está concluída. As análises das 13 fontes e a consolidação GPT × Claude × Manus permanecem em `AnálisedeCodigos/` e em `ROADMAP_BRAIM_CONSOLIDADO.md`.

A partir da Fase C, o runtime executável do Brain é implementado em Python 3 com biblioteca padrão, como uma camada de referência testável independente do esqueleto Kotlin existente. O runtime preserva os limites arquiteturais do roadmap: Policy é deny-by-default, EventStore é append-only, Sandbox não chama IA e cada módulo é substituível por interfaces.

Cada fase é enviada em commit separado para permitir revisão e rollback independentes.

## Atualização desta rodada — 2026-09-12

Foram implementados reforços adicionais no runtime Python: validação de contratos e versões de schema; decomposição determinística, validação de dependências e loop de validação com retry; emissão de `PlanCreated` no pipeline; memória semântica com provenance e bloqueio de secrets; hash, isolamento de path e permissões para skills; preservação de quota, cooldown e health tracking no catálogo de APIs; validação de grafos e persistência atômica de workflows; e testes de regressão para essas áreas.

## Atualização de execução segura — 2026-09-12

O Sandbox agora suporta cancelamento cooperativo, timeout com encerramento do grupo de processos, limites de recursos e retorno explícito de estados `CANCELLED` e `TIMEOUT`. Workflows podem receber um `PolicyBroker` e são bloqueados quando qualquer capability do grafo não é autorizada. Foram adicionados testes E2E para cancelamento, policy de workflow e preservação de quota.

## Atualização de discovery e extensibilidade — 2026-09-12

Foi adicionada uma camada de pesquisa com fontes HTTPS, evidências com hash, confiança e validação de suficiência. Os adapters GitSkills e Anthropic agora exigem metadados de licença, persistem hash do corpo e mantêm skills não verificadas em quarentena. A descoberta dinâmica de APIs valida licença, custo, probe e provenance antes de registrar candidatos no catálogo.

## Atualização de aprovação e recovery — 2026-09-12

O runtime agora possui `ApprovalStore` persistente com decisões de uso único e expiração, `CredentialVault` baseado exclusivamente em referências, lock de arquivo para append multi-processo no EventStore e `StateReconstructor` para reconstrução de execução por replay de eventos. A suíte cobre concorrência, aprovação, isolamento de credenciais e recovery.

## Atualização de qualidade e observabilidade — 2026-09-12

Foram adicionados correlation IDs, spans e métricas; `LearningStore` e `FeedbackLoop` append-only com evidência de validação; `DeliveryPipeline` com QA gate obrigatório; e validação de configuração deny-by-default com isolamento de paths. A suíte cobre essas integrações e totaliza 32 testes.

## Atualização de hardening — 2026-09-12

Foi adicionado um registry versionado de contratos com campos deprecated, lease expirável para workflows concorrentes e guards adversariais contra traversal, secrets em texto e marcadores de prompt injection. A suíte agora totaliza 35 testes.

## Atualização de integração crítica — 2026-09-12

O pipeline passou a integrar o `ApprovalStore` ponta a ponta: decisões `ASK` geram solicitações persistidas, pausam a execução e permitem retomada com o mesmo `run_id`, `task_id` e `step_id`, com validação de TTL, capability, resource e uso único. Também foram integrados os eventos `ApprovalGranted` e `ApprovalDenied`.

Foi implementado o ciclo de validação com `Critic`, diagnóstico estruturado, `CorrectionRequested`, `Retry`, limite de tentativas e eventos `ValidationPassed`/`Delivered`. O `EventStore` agora valida o schema dos eventos, recupera uma última linha JSONL parcialmente escrita e reconstrói estado de aprovação e entrega por replay. Contratos receberam payload formal, campos obrigatórios, migração inicial e registry de contratos principais.

O sandbox recebeu limites de processos, descritores, tamanho de arquivo/disco, extensões e detecção de argumentos com metacaracteres. Essas medidas são hardening defensivo, não substituem namespace de rede/processo, jail de filesystem ou container OS-level; esses itens continuam pendentes para uma implantação com privilégios e runtime apropriados.

Esta rodada adicionou testes de integração para approval, correction loop, anti-replay, recuperação de eventos e argumentos inseguros. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 39 testes, todos aprovados.

## Atualização de isolamento OS-level — 2026-09-12

O `SandboxExecutor` passou a iniciar jobs, quando suportado pelo host, em namespaces de usuário, montagem e PID via `unshare`, com diagnóstico explícito no resultado. A solicitação de namespace de rede agora é tratada de forma fail-closed quando `isolation_required=True`: neste ambiente a operação é recusada porque o kernel não permite `unshare --net`; não há indicação falsa de que o tráfego esteja isolado. Quando o isolamento estrito não é solicitado, o executor mantém fallback compatível com limites de recursos e informa a degradação.

Foram adicionados testes para confirmar a execução em namespace e a não degradação silenciosa da política de rede. A suíte passou a totalizar 41 testes aprovados. Filesystem jail/chroot e cgroup de processos continuam sendo responsabilidade da implantação/container host; o runtime não simula essas garantias apenas com validação de paths.

## Atualização do engine de workflows — 2026-09-12

O engine de workflows passou a persistir estados explícitos de execução, incluindo `running`, `blocked`, `failed`, `paused`, `cancelled`, `timed_out` e `compensating` quando aplicáveis. O cancelamento pode ser propagado por `cancel_event` durante a execução de um node, e o timeout global é verificado antes de cada transição.

Retry agora registra tentativas, aceita backoff exponencial limitado e valida o output de cada node por schema declarativo ou callback. Inputs também podem ser validados antes do dispatch. Workflows que configurarem nodes compensáveis podem executar uma compensação reversa após cancelamento ou falha, com persistência atômica do estado intermediário. A compatibilidade com handlers booleanos e idempotência existentes foi preservada.

Foram adicionados testes para cancelamento com compensação, validação de input/output e retry por falha de contrato. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 44 testes aprovados.

## Atualização de research e roteamento — 2026-09-12

A camada de research passou a ranquear evidências por confiança e frescor, preservar provenance e hash de conteúdo e colocar fontes com marcadores de prompt injection em quarentena. O pipeline pode receber uma `ResearchLayer` e fontes HTTPS, registrar `ResearchCollected` e incluir evidências delimitadas como dados não confiáveis no prompt, sem tratá-las como instruções.

O catálogo de APIs passou a oferecer seleção por qualidade, confiabilidade, latência, custo e disponibilidade; reserva e reconciliação de quota antes e depois do dispatch; timestamp de probes; e fallback somente entre entradas que declaram a capability equivalente. Credential references continuam sendo referências, sem exposição de secrets.

Foram adicionados testes de quarentena de research, provenance, reserva de quota, seleção de provider equivalente e integração da evidência ao pipeline. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 48 testes aprovados.

## Atualização de memória e learning — 2026-09-12

A memória SQLite passou a deduplicar experiências por hash de conteúdo, preservar provenance, suportar `expires_at` e remoção de registros expirados, além de manter WAL e locks para concorrência local. Conteúdo com secrets ou marcadores de prompt injection é rejeitado antes da persistência.

O `LearningStore` agora mantém hash de conteúdo, versão, provider, agent e skill, com deduplicação append-only e validação de métricas e evidências. O `ExecutionLearningBridge` conecta resultados de execução à memória e ao learning record, incluindo custo, latência, evidências e política de retenção. O pipeline aceita esse bridge opcional e emite `LearningRecorded` ou `LearningRejected`.

Foram adicionados testes de deduplicação, retenção, provenance, rejeição de injection/secrets e persistência integrada. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 51 testes aprovados.

## Atualização de contratos e configuração — 2026-09-12

O `ContractRegistry` passou a fornecer schemas formais para `TaskSpec`, `Plan`, `PlanStep`, `ExecutionRequest`, `ExecutionResult`, `SandboxJob`, `SandboxJobResult` e `Event`, incluindo campos obrigatórios, versão e validação de tipos básicos. A compatibilidade com schemas customizados e campos deprecated foi preservada.

Foi adicionado o `ConfigurationManager`, que carrega seções JSON ou TOML da pasta de configuração, valida deny-by-default, timeout e ausência de secrets, e só substitui o snapshot depois que a nova configuração está totalmente validada. O reload é seguro e atômico em relação aos leitores.

Eventos agora possuem `correlation_id` persistido, com fallback para `run_id` em logs legados. Foram adicionados contract tests, testes de reload e teste de persistência de correlação. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 54 testes aprovados.

## Atualização de observabilidade operacional — 2026-09-12

A camada de observabilidade passou a suportar gauges, alertas correlacionados e exportação JSON estruturada contendo spans, counters, gauges e alerts. O pipeline aceita `Observability`, gera spans por execução e retry, mede o status dos resultados e emite alerta específico para decisões de Policy negadas.

O `DeliveryPipeline` aceita telemetria e `TraceContext`, registra spans do QA gate e counters separados para validação recusada e entrega concluída. A correlação entre trace, run, sessão e task é preservada nos spans exportados. Foram adicionados testes de exportação, alertas de Policy, spans do pipeline e métricas de delivery. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 57 testes aprovados.

## Atualização final de hardening — 2026-09-12

O EventStore recebeu sequência por stream, rotação por tamanho, compactação com reconstrução da hash chain, recovery de linha parcial e migração de payloads. Workflows receberam renovação explícita de lease, cancelamento persistente e histórico de transições. O `CredentialVault` agora possui um dispatcher wrapper que injeta o secret somente na chamada de execução, mantendo a request e os eventos sem o valor sensível.

Skills externas passaram a validar licença contra allowlist, executar scan de conteúdo, registrar URL/commit/signature e exigir assinatura quando metadados imutáveis são fornecidos. O registry suporta quarentena persistente e revogação que impede novas autorizações. Compatibility mode para manifests legados verificados permanece disponível para migração gradual.

Foram adicionados testes finais para streams, compactação, leases, credentials e skills assinadas. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 61 testes aprovados.

Permanecem dependências de implantação que não podem ser simuladas com segurança em Python puro: namespace de rede quando o kernel o bloqueia, filesystem jail/chroot real, cgroups, isolamento multiprocesso entre máquinas e verificação criptográfica baseada em uma autoridade de assinatura externa. O runtime trata esses casos com fail-closed ou expõe interfaces explícitas para o host/container fornecer a garantia.

## Atualização de fronteiras de implantação — 2026-09-12

Foi adicionado `HostCapabilityProbe` para reportar capacidades reais do host, incluindo `unshare`, Bubblewrap, cgroup v2, permissões de escrita e user namespaces. O sandbox aceita filesystem jail via Bubblewrap quando disponível e um `cgroup_path` opcional; em modo estrito, a ausência de qualquer controle solicitado rejeita a execução em vez de simular isolamento.

Foi adicionado o `SecureIPC`, um protocolo de socket Unix com framing de tamanho, nonce, HMAC e sanitização de credenciais para o boundary Brain/Sandbox. O `DistributedLeaseStore` fornece leases transacionais SQLite com renovação, release e fencing tokens monotônicos, e o `WorkflowEngine` aceita esse backend opcional para coordenação entre processos ou hosts que compartilhem o armazenamento transacional.

O `SignatureVerifier` define uma interface fail-closed para chaves Ed25519 confiáveis e mantém a decisão de confiança fora do manifesto da skill. Foram adicionados testes de fencing tokens, autenticação IPC, sanitização e probe de capacidades. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 65 testes aprovados.

## Atualização de binding e boundary de execução — 2026-09-12

O pipeline passou a criar um `ExecutionBinding` imutável após a decisão de Policy, cobrindo run, task, step, capability, provider, resource e decision ID. O digest é incluído nos inputs e no evento `AgentDispatched`, permitindo detectar troca de provider, capability ou recurso entre autorização e dispatch.

O QA gate pode ser conectado diretamente ao pipeline antes do correction loop, de modo que resultados sem evidência ou sem output requerido não sejam entregues como sucesso. O importador de skills externas aceita uma `SignatureVerifier` Ed25519 opcional e, quando configurada, exige uma autoridade de chave confiável em vez de confiar apenas no manifesto.

Foram adicionados testes de digest anti-tampering, binding persistido e QA gate integrado. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 68 testes aprovados.

## Atualização de execução E2E e recovery — 2026-09-12

Foi adicionada a fachada `RuntimeCoordinator`, que encadeia pipeline, QA/delivery, eventos, observabilidade e replay de estado. Ela persiste `JobCompleted`, `JobFailed` e `JobCancelled`, suporta cancelamento cooperativo e timeout entre etapas e oferece `recover()` e `replay()` para reconstrução após falhas.

O runtime também aceita fault injection por estágio, permitindo testar crash antes e depois de pipeline/delivery sem ocultar a transição no EventStore. O `StateReconstructor` passou a reconhecer estados finais de job, além de approval, validation e delivery.

Foram adicionados testes E2E para o fluxo completo, crash injetado recuperável por replay e cancelamento persistido. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 71 testes aprovados.

## Atualização de security suite e golden events — 2026-09-12

Foi adicionada a validação `golden_events`, que verifica o contrato completo, a sequência, a correlação e a hash chain sem depender de timestamps fixos. A suíte cobre recovery após uma última linha JSONL parcial, integridade após replay e append concorrente de múltiplos processos com idempotency keys distintas.

As fronteiras de confiança agora possuem auditoria recursiva para secrets por chave e prompt injection em objetivo, contexto, skill e memória. O objetivo é rejeitado antes da classificação, contexto é rejeitado antes da construção do prompt e eventos permanecem redacted antes da persistência.

Foram adicionados testes de golden events, crash recovery, concorrência multiprocesso, vazamento de credentials e injection. A suíte executada com `python3 -W error::ResourceWarning -m unittest discover -s tests -q` totaliza 74 testes aprovados.

## Atualização de integração operacional — 2026-09-12

Foram adicionados wrappers de telemetria para Policy, Approval, Planner, Router, API Catalog e Learning, com spans, counters e gauges por decisão, request, seleção, latência e registro. O `SandboxDispatcher` valida o digest do `ExecutionBinding`, correlação de run/session e só então cria o `SandboxJob`, convertendo o resultado para `ExecutionResult` com evidência e provenance.

A configuração agora possui `validate_cross_component`, que rejeita conflitos entre network/filesystem do Sandbox e Policy, capabilities roteadas fora da allowlist e references de credenciais não declaradas. Foram adicionados testes de boundary tamperado, execução válida no Sandbox, Approval telemetry e métricas transversais. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 78 testes aprovados.

## Atualização de research externa e routing — 2026-09-12

A `ResearchLayer` agora possui `HTTPSResearchFetcher` com HTTPS obrigatório, bloqueio de credenciais na URL, allowlist opcional de hosts, resolução DNS com rejeição de loopback/private/link-local/reserved/multicast, bloqueio de redirects, timeout, limite de bytes e validação de content type. Fontes aceitas recebem `retrieved_at`, hash de conteúdo e provenance preservada; injection continua sendo colocada em quarentena.

O `RoutedDispatcher` reserva quota antes do dispatch, reconcilia no retorno, atualiza confiabilidade/latência do provider e inclui custo estimado, duração e provider nas métricas do `ExecutionResult`. Falhas liberam a reserva e não fazem fallback fora da capability roteada.

Foram adicionados testes de SSRF, esquema inseguro, limite de excerpt, provenance, quarentena de conteúdo externo e reserva/reconciliação com custo. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 82 testes aprovados.

## Atualização de retomada e fallback autorizado — 2026-09-12

O `ApprovalStore` agora pode reidratar requests a partir de eventos persistidos, e o pipeline reconstrói o contexto mínimo da task quando um processo novo chama `resume`. Eventos `ApprovalGranted` e `ApprovalDenied` bloqueiam qualquer segunda retomada, preservando anti-replay após restart.

Foi adicionado `AuthorizedFallback`, que reautoriza cada provider candidato com a mesma capability, actor, run e task, registra cada tentativa e usa idempotency keys por provider. Providers negados pela Policy não são chamados, e apenas resultados de providers explicitamente autorizados podem ser retornados.

Foram adicionados testes de restore append-safe, anti-replay, reautorização por provider e idempotência. A suíte executada com `python3 -m unittest discover -s tests -q` totaliza 84 testes aprovados.
