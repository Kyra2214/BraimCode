# BrainCode — Roadmap Canônico

Este arquivo substitui listas paralelas de ideias como referência para o próximo trabalho.

## Marco 0 — Consolidação

- [x] Remover arquitetura local-LLM do produto.
- [x] Definir Agent bounded.
- [x] Consolidar capability como unidade universal.
- [x] Consolidar Registry/Discovery.
- [x] Consolidar PolicyBroker.
- [x] Consolidar ActionGateway.
- [x] Consolidar SkillRegistry.
- [x] Documentar fronteiras.
- [x] Remover snapshots IaBrain sem uso.

## Marco 1 — Fechar o caminho de execução

- [ ] mapear cada entrada real do Chat até Brain;
- [ ] garantir que ações reais passem por Discovery → Policy → Gateway;
- [ ] garantir que nenhum caminho paralelo execute capacidade diretamente;
- [ ] adicionar testes de wiring/orphan para os componentes do núcleo;
- [ ] registrar evidência observável de cada execução relevante.

## Marco 2 — Retrieval e conhecimento

- [ ] implementar executor de `retrievalHints`;
- [ ] recuperar fonte antes de chamar API novamente quando houver hint confiável;
- [ ] deduplicar conhecimento;
- [ ] versionar correções;
- [ ] estruturar citações/evidências;
- [ ] elevar confiança somente após validação executável ou cross-check.

## Marco 3 — Planejamento e tarefas complexas

- [ ] consolidar Planner → ExecutionPlan;
- [ ] Function Splitter declarativo;
- [ ] Dispatcher único;
- [ ] Workflow/DAG para dependências, paralelismo, retry, timeout e cancelamento;
- [ ] JobStore para tarefas longas/persistentes.

## Marco 4 — Skills e aprendizado operacional

- [ ] Skill criada a partir de conhecimento somente após validação;
- [ ] registrar procedimento + pré-condições + capabilities + evidência;
- [ ] Skill Registry com lifecycle e versão;
- [ ] impedir skill inválida de entrar no caminho automático.

## Marco 5 — Segurança de execução

- [ ] OS-level filesystem/network/process isolation quando disponível;
- [ ] enforcement de CPU/memória/PIDs/FDs/disco;
- [ ] trust chain de RootFS;
- [ ] credential binding seguro;
- [ ] SSRF/DNS rebinding hardening;
- [ ] testes arquiteturais de bypass.

## Regra de prioridade

**Não criar mais arquitetura antes de fechar as conexões existentes.**

A prioridade é reduzir duplicação, aumentar wiring real, testar caminhos completos e somente depois adicionar novas abstrações.
