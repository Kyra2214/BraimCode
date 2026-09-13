# Auditoria Pesada — BrainCode

**Atualização:** 2026-09-13  
**Escopo:** `brain_runtime/`, `:brain`, `:android-module`, `:app`, execução, autorização, toolchains, Security Lab, contratos e documentação.

## Regra da auditoria

> **Implementação + teste + caller real + evidência observável + documentação coerente.**

Uma pendência não é considerada resolvida apenas porque foi renomeada, reclassificada ou retirada do backlog.

## Correções reais aplicadas após a auditoria anterior

### 1. Authorization Python — corrigido

`brain_runtime/authorization.py` não usa mais `@dataclass` nem oferece construtor funcional ao consumidor. A emissão normal usa `_issue()` e cria a capability com identidade do `BrainPipeline`, modo e nonce aleatório. Tentativas de construir `ExecutionAuthorization(...)` diretamente agora falham com `TypeError`.

**Limite:** Python continua sendo um processo confiável único; código com acesso deliberado ao mesmo processo pode inspecionar/alterar objetos privados. Isso não substitui isolamento de processo.

### 2. Authorization Kotlin — corrigido contra adulteração por `copy()`

`PolicyDecision` agora carrega `AuthorizationToken`. O token tem construtor privado e emissão `internal` no módulo `:brain`. `ExecutionAuthorization.fromDecision()` exige `ALLOW`, TTL válido, token presente e correspondência do token com os campos relevantes.

O token é vinculado a:

- decisionId;
- runId/taskId/actor;
- capability/resource;
- risk/approval/sandbox;
- network policy;
- filesystem roots;
- budget;
- expiry.

Assim, `decision.copy(capability=..., networkAllowed=..., budget=..., ...)` não transforma a cópia em autorização válida.

Foi adicionado teste específico de adulteração em `PolicyBrokerTest`.

### 3. Superfície de comando do agente — corrigida

`AgentSandboxSession.rodarComando()` deixou de ser API pública entre módulos e passou a `internal rodarComandoInterno()`.

A API destinada ao agente é `rodarCapacidade()`, que obrigatoriamente passa pelo `CapabilityResolver`.

### 4. Capability genérica `sandbox.run` — removida

O `CapabilityResolver` não oferece mais uma capability que transforma parâmetros do agente em comando arbitrário via `sandbox-run`.

O catálogo atual é composto pelas capacidades explícitas de build/test/health/info/diagnose/clean.

Isso elimina a válvula de escape que havia sido mascarada como uma capability válida.

### 5. Toolchain rollback — corrigido semanticamente

O snapshot anterior registrava simplesmente os pacotes declarados e o rollback podia removê-los mesmo quando já existiam antes.

Agora o detector captura, antes da instalação, somente os pacotes do perfil realmente instalados e suas versões. O rollback:

1. identifica pacotes adicionados pela transação;
2. remove somente esses pacotes;
3. tenta reinstalar os pacotes previamente existentes com suas versões observadas;
4. só limpa o snapshot quando a restauração termina com sucesso.

Se a versão anterior não estiver disponível no repositório de pacotes, o rollback falha explicitamente em vez de fingir restauração completa.

## Problemas que continuam reais — não foram mascarados

### P0 — Isolamento de rede

`networkAllowed=false` continua sendo uma decisão de Policy, mas `proot` sozinho não cria namespace de rede isolado. Portanto não é correto chamar isso de isolamento de segurança do host.

**Próxima implementação correta:** mecanismo OS-level verificável; se indisponível, o caminho protegido deve falhar fechado.

### P0 — Jail de filesystem

O canonical-path/workspace check protege a API da aplicação, mas não constitui uma fronteira contra acesso a `/proc`, `/sys`, `/dev`, `/data`, `/sdcard`, mounts e equivalentes.

**Próxima implementação correta:** jail OS-level verificável e testes de escape.

### P0 — Árvore de processos

`destroy()` seguido de `destroyForcibly()` atua sobre o `Process`, mas não prova que descendentes tenham sido encerrados.

**Próxima implementação correta:** process group/session e kill do grupo quando suportado pela plataforma.

### P0 — Recursos OS-level

O runtime possui timeout e collector com limite de saída, mas não há evidência de enforcement OS-level completo de CPU, memória, PIDs, FDs, disco e árvore de processos.

### P1 — Trust chain

Hashes dos RootFS verificam integridade do artefato, mas não constituem autenticação da autoridade que publicou o manifest. Os RootFS migrados do SandBox permanecem copy-only e não devem ser reconstruídos.

### P1 — Skills

Hash/proveniência/licença e checks locais existem, mas metadados fornecidos pelo próprio skill não devem ser tratados como autoridade final de confiança.

### P1 — Planner/executor

A arquitetura correta permanece:

```text
Task
 ↓
Planner
 ↓
ExecutionPlan
 ↓
PolicyBroker
 ↓
AuthorizedPlan
 ↓
Agent
 ↓
CapabilityResolver
 ↓
Sandbox
```

Ainda é necessário um teste de arquitetura que prove que nenhum executor consegue reconstruir uma execução diretamente do `Task` original, ignorando o plano autorizado.

### P1 — Credenciais

Credential refs precisam continuar vinculados a run/agent/capability/provider/purpose/expiry, com subprocesso tratado como fronteira de exfiltração.

### P1 — SSRF

O caminho de pesquisa/HTTP ainda precisa de proteção contra DNS rebinding: validar o destino não basta se a conexão posterior puder resolver para outro endereço.

### P1 — Persistência/eventos

A fachada já usa `FileEventStore` para eventos locais. Continua pendente provar rotação/compactação sem quebrar a continuidade verificável do hash-chain quando essa operação existir.

### P1 — Leases

Workflow leases ainda precisam de fencing token para impedir que uma execução antiga reassuma o recurso após perda/renovação concorrente.

## UI

A UI operacional ainda está em construção. Portanto o antigo item de “terminal livre da UI” **não é tratado como pendência atual de produto**.

Isso não autoriza shell arbitrário no núcleo: a superfície do agente já foi fechada para `rodarCapacidade()` e `sandbox.run` foi removida do catálogo.

Quando a UI operacional for construída, ela deverá nascer sobre essas APIs e não reintroduzir terminal arbitrário fora da Policy.

## Security Test Lab

O corpus atual é **sintético/determinístico**. Isso é deliberado para regressão offline segura. Ele não deve ser descrito como execução de payloads adversariais reais.

O fluxo persistente implementado é:

```text
SecurityProjectScanner
 ↓
SecurityScenarioCatalog
 ↓
SecurityRegressionCorpus.runDeterministic()
 ↓
SecurityAssessmentEngine
 ↓
SecurityRegressionCorpus.record(observedResults)
```

Ainda falta uma camada de testes adversariais controlados em ambiente realmente isolado antes de usar o laboratório como evidência de isolamento OS-level.

## Workflow da fachada

`BrainIntegrationFacade.runHealthWorkflow()` permanece workflow local/demonstrativo. Ele não é contado como execução real Brain → Policy → Sandbox.

Isso está documentado como tal e não deve ser usado como evidência de integração vertical.

## Estado de integração

Existe integração vertical real em partes do Android, especialmente no caminho de health/plan/capability/Sandbox. Entretanto, nem toda API do `:brain` participa do fluxo Android atual.

Não há justificativa para forçar `BrainExecutionCoordinator`, prompt generator ou APIs avançadas à UI apenas para marcar “integrado”. Eles permanecem APIs internas até haver caso de uso real.

## Validação

As alterações desta rodada foram publicadas diretamente no repositório `Kyra2214/BrainCode`.

**Importante:** esta rodada foi realizada por inspeção e escrita no repositório. Não foi executado Gradle, suíte Python ou APK ARM64 através do conector nesta rodada. Portanto não há novo “BUILD PASS” ou “TEST PASS” sendo alegado aqui.

A próxima validação obrigatória é executar:

- testes `brain_runtime`;
- testes `:brain`;
- testes `:android-module`;
- testes `:app`;
- build Debug;
- validação funcional em ARM64.

## Resultado honesto

Os problemas que eram realmente corrigíveis no código e estavam sendo tratados como resolvidos por documentação foram corrigidos agora: **autorização, adulteração de decisão, superfície de comando do agente, capability genérica e rollback de toolchain**.

Os problemas que dependem de isolamento OS-level continuam explicitamente abertos. Não foram renomeados para “resolvidos”.
