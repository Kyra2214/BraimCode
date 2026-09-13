# Tarefas Pendentes — BrainCode

## Estado consolidado — 2026-09-13

Critério rígido:

> **Implementação + teste + caller real + evidência observável + documentação coerente.**

Esta lista não fecha uma pendência por simples reclassificação documental. Uma correção só é marcada como fechada quando existe mudança real no código ou quando a funcionalidade é explicitamente fora do escopo atual.

## Corrigido nesta rodada

- [x] **ExecutionAuthorization Python** não possui mais construtor utilizável por consumidores; a emissão normal ocorre somente por `_issue()`.
- [x] **ExecutionAuthorization Kotlin** rejeita decisões sem token do `PolicyBroker`.
- [x] **PolicyDecision Kotlin** carrega token opaco emitido pelo broker; `copy()` adulterando capability, recurso, budget, rede, roots, TTL ou demais campos não produz autorização válida.
- [x] **Teste de adulteração da PolicyDecision** adicionado em `PolicyBrokerTest`.
- [x] **AgentSandboxSession** não expõe mais `rodarComando()` como API pública entre módulos; a superfície do agente é `rodarCapacidade()`.
- [x] **CapabilityResolver** não possui mais `sandbox.run` como capability genérica de comando arbitrário.
- [x] **ToolchainTransactionStore** passou a registrar os pacotes realmente instalados antes da mudança, incluindo versão.
- [x] **Toolchain rollback** passou a remover somente pacotes adicionados pela transação e tentar restaurar as versões observadas anteriormente.
- [x] **SecurityRegressionCorpus** recebe resultados observados explicitamente e `SandboxPlatform.runSecurityRegression()` executa o fluxo scanner → probes → assessment → corpus.
- [x] **Eventos locais da fachada** usam armazenamento persistente `FileEventStore`.
- [x] **Workflow da fachada** continua explicitamente local/demonstrativo; não é apresentado como execução Sandbox real.
- [x] **UI ainda não é tratada como pendência de arquitetura**: a interface operacional ainda está em construção e não é usada como evidência de segurança do núcleo.

## Ainda pendente — núcleo real

### P0 — Segurança de execução

- [ ] **Isolamento de rede real:** `networkAllowed=false` ainda precisa de mecanismo OS-level verificável. Proot sozinho não fornece namespace de rede isolado. Até existir enforcement verificável, nenhum documento deve afirmar que o deny de rede é uma barreira de segurança do host.
- [ ] **Jail filesystem real:** validar e implementar uma fronteira OS-level para impedir escapes por `/proc`, `/sys`, `/dev`, `/data`, `/sdcard`, mounts e caminhos equivalentes. O workspace/canonical-path check atual é proteção de aplicação, não sandbox de segurança.
- [ ] **Process tree isolation:** `destroy()/destroyForcibly()` ainda não prova encerramento de descendentes. Implementar process group/session e kill do grupo quando a plataforma permitir.
- [ ] **Resource enforcement:** timeout e limite de stdout/stderr existem, mas ainda faltam garantias OS-level demonstráveis para CPU, memória, PIDs, FDs, disco e árvore de processos.
- [ ] **Output hard cap no transporte:** o collector limita o buffer, mas a validação precisa demonstrar que um processo que produz saída continuamente não consegue consumir memória ilimitada antes da coleta.
- [ ] **RootFS trust chain:** checksum garante integridade, mas a origem/trust do manifest ainda precisa de assinatura/autenticidade verificável.
- [ ] **Skills trust:** metadados declarados pelo próprio skill não devem ser suficientes para atribuir confiança; falta autoridade/registry de confiança verificável.

### P1 — Integração do núcleo

- [ ] **Planner → ExecutionPlan → PolicyBroker → AuthorizedPlan → Agent → Sandbox:** garantir por testes de arquitetura que o executor nunca reconstrói uma execução a partir do `Task` original ignorando o plano autorizado.
- [ ] **Credenciais:** bind de credential refs ao run/agent/capability/provider/purpose/expiry e controle explícito da fronteira de exfiltração por subprocesso.
- [ ] **SSRF/DNS rebinding:** pesquisa/HTTP precisa validar e manter o destino efetivamente conectado, não apenas o IP obtido em uma etapa anterior.
- [ ] **EventStore:** rotação/compactação deve preservar continuidade verificável do hash-chain.
- [ ] **Workflow leases:** adicionar fencing token para evitar execução concorrente de uma lease antiga.
- [ ] **Orphan/wiring tests:** teste automatizado para detectar componentes críticos sem caller real ou caminhos que contornem Policy/Sandbox.

### P2 — Validação

- [ ] Reexecutar suíte Python completa.
- [ ] Reexecutar testes `:brain` Kotlin/JVM.
- [ ] Reexecutar testes `:android-module`.
- [ ] Reexecutar testes `:app`.
- [ ] Build Debug Android.
- [ ] Validar APK em ARM64 real/emulador.
- [ ] Validar RootFS/proot, `sandbox.health`, approval/resume, cancelamento, interrupção e recovery.

## Fora do escopo Android offline atual

- APK Release/assinatura/keystore de produção.
- Servidor/backend distribuído.
- Postgres/Redis/etcd.
- Multi-host.
- Providers externos/transporte remoto.
- Reconstrução ou alteração dos RootFS homologados migrados do SandBox.

## Observação importante

O terminal livre da UI não é tratado como pendência atual porque a UI operacional ainda está em construção. Isso **não** significa que shell arbitrário seja uma capacidade válida do núcleo: a superfície entre módulos agora é `rodarCapacidade()` e o catálogo não oferece `sandbox.run`.

A ausência de isolamento OS-level de rede/filesystem/processos continua sendo uma limitação técnica real e permanece explicitamente pendente.

## Referências

- `AUDITORIA_PESADA.md`
- `docs/MAPA_INTEGRACAO_2026-09-13.md`
- `docs/APRESENTACAO_BRAINCODE.md`
- `docs/BACKLOG_OFFLINE_ITEM_5.md`
- `docs/SECURITY_TEST_LAB.md`
- `docs/SANDBOX_RELEASE_MIGRATION.md`
