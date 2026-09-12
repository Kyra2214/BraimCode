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
