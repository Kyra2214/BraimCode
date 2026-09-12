# Plano de integração — Frente 1 (SandBox) + Frente 2 (BraimCode)

> Projeto em 3 frentes, nesta ordem: **1) SandBox → 2) BraimCode → 3) Interface.**
> Este documento cobre a junção das frentes 1 e 2. A frente 3 só começa depois
> desta junção estar consolidada.

## O app, em uma frase

Um "Codex para Android" cujo diferencial é que o usuário nunca escolhe API,
modelo ou agente diretamente. O **Brain** decide isso por ele: tem a ideia,
prepara o terreno onde os agentes vão trabalhar, e revisa o resultado antes
de qualquer coisa voltar pro usuário. O Brain é a entrada e a saída do
processo — o usuário só fala com ele.

## Modelo arquitetural

O Sandbox **não é um executor de comandos avulsos**. Ele é o ambiente
operacional controlado onde os agentes vivem e trabalham: filesystem
isolado, processos isolados, ferramentas permitidas, recursos limitados,
rede conforme política, workspace, runtime, coleta de evidências.

```
Usuário → Brain (ideia, escolha de API/skill, prepara terreno)
        → PolicyBroker → ExecutionAuthorization
        → Sandbox.abrirSessao() → Agente trabalha dentro do terreno preparado
        → Runtime isolado executa
        → Resultado/evidências → Brain revisa (Validator/Critic)
        → Usuário recebe saída
```

Divisão de papéis:
- **IaBrain** decide O QUE fazer.
- **Agente** decide COMO trabalhar dentro das capacidades permitidas.
- **Sandbox** controla ONDE e SOB QUAIS LIMITES isso acontece.
- **Runtime** (proot, via `SandboxRuntime`) executa efetivamente.

O agente não "manda algo pro Sandbox" de fora — ele vive dentro do domínio
que o Sandbox já preparou, autorizado pelo PolicyBroker.

## Por que tudo dentro de um único app

BraimCode e SandBox eram dois repositórios separados. Decisão explícita:
**não haverá processo separado nem IPC entre dispositivos.** O Brain é
incrementado dentro do mesmo app Android do SandBox, no mesmo processo,
chamando o Sandbox real por função Kotlin direta — sem rede, sem
serialização de transporte.

Isso também resolve uma pista que já estava no próprio BraimCode: o pacote
`com.brain.*` sempre foi Kotlin, vivendo numa pasta `app/` sem nenhum
projeto Gradle — só nunca teve um app real para entrar. O runtime Python
(`brain_runtime/`, guardado em `reference/braincode-python/` neste
repositório) nunca foi o destino final: é a **especificação por testes**
(112 testes) que a versão Kotlin precisa cobrir, não código para copiar.

## Etapas

### Etapa 1 — Trazer o BraimCode pro Gradle do SandBox *(feita nesta rodada)*
Novo módulo `:brain` (Kotlin/JVM puro, sem plugin Android — nenhum arquivo
`com.brain.*` importa `android.*`), com o código movido de
`BraimCode/app/src/main/kotlin/com/brain/` e os catálogos JSON movidos de
`assets/` para `brain/src/main/resources/catalog/`. `:android-module` já
depende de `:brain` (dependência de compilação preparada; a ponte real
ainda não foi escrita).

Critério de pronto: `./gradlew :brain:compileKotlin` compila sem tocar em
`:android-module` nem `:app`.

### Etapa 2 — PolicyBroker e ExecutionAuthorization (o "terreno preparado") *(feita nesta rodada)*
Novo pacote `com.brain.policy` dentro de `:brain`:
- `Decision` (ALLOW/ASK/DENY), `ApprovalRequired` (NONE/USER/ADMIN), `PolicyContext`
  e `PolicyDecision` — espelhando `models.py` do runtime Python de referência.
- `RiskClass` (em `com.brain.execution`) ganhou `READ_ONLY`, que faltava para
  preservar a mesma isenção de sandbox obrigatório que a Policy Python trata
  para `LOW`/`READ_ONLY`.
- `PolicyBroker.authorize(...)`: mesma ordem de verificação do `policy.py`
  original (TTL → budget → capability registrada → actor autorizado →
  sandbox obrigatório por risco → rede → filesystem → expiração → aprovação
  → ALLOW), deny-by-default preservado.
- `ExecutionAuthorization`: só nasce de uma `PolicyDecision` com `ALLOW`
  (via `fromDecision`, que retorna `null` para ASK/DENY/expirado). É uma
  classe comum, não `data class` — um `data class` com construtor privado
  ainda expõe `copy()` público, o que abriria uma forma de montar uma
  autorização adulterada sem passar pela Policy.
- Testes em `PolicyBrokerTest.kt` espelhando `test_policy_events.py` e
  cobrindo os casos novos (rede, filesystem, expiração, budget negativo,
  `ExecutionAuthorization.fromDecision`).

Base transversal — sem isso as etapas seguintes não têm o que consumir.

### Etapa 3 — `AgentSandboxSession`
Acima do `SandboxRuntime` existente (motor de baixo nível, comando via
proot), em `:android-module`:

```kotlin
Sandbox.abrirSessao(autorizacao: ExecutionAuthorization): AgentSandboxSession
```

A sessão fixa workspace dentro do rootfs, ferramentas/skills permitidas,
rede conforme a autorização, budget/timeout total, e expõe primitivas que o
agente chama repetidamente durante o trabalho: `rodarComando(...)`,
`lerArquivo(...)`, `escreverArquivo(...)`, `listarArquivos(...)`. O agente
nunca fala com `SandboxRuntime` diretamente, só com a sessão.

Critério de pronto: abrir uma sessão com autorização fixa e rodar 2-3
chamadas em sequência no mesmo workspace, confirmando que arquivo escrito
num passo é lido no próximo.

### Etapa 4 — CapabilityResolver
`Requisito(capacidade)` não é comando de shell, mas o rootfs só entende
comando. Um `CapabilityResolver` traduz capacidade + parâmetros em comando
real, restrito ao catálogo de ferramentas que o SandBox já lista (plugins
Node, testes, etc). Começa só com o que esse catálogo já cobre.

### Etapa 5 — Router/escolha de API *(feita nesta rodada)*
`DefaultAIRouter` + `ApiCatalog` já existiam em Kotlin em `:brain` (só não
compilavam até a Etapa 1); o trabalho desta rodada foi cobrir com teste
(`DefaultAIRouterTest`, `InMemoryApiCatalogTest`, `ApiCatalogLoaderTest`,
em `brain/src/test/kotlin/com/brain/router/`) e documentar as limitações
conscientes em relação ao `apis.py`/`DynamicApiCatalog` de referência
(ver KDoc de `AIRouter.kt`): sem reserva de quota, sem cooldown
automático, sem `waterfall()` e sem persistência entre reinícios — tudo
isso fica para a Fase G / Etapa 7, e nenhum bloqueia a Etapa 6. Usuário
nunca vê essa escolha — o Router recebe a capacidade definida pelo
Planner e escolhe provider/modelo por qualidade, confiabilidade
(histórico via `LiveStats`), velocidade e fallback.

Critério de pronto: `./gradlew :brain:test --tests "com.brain.router.*"`
passando, sem tocar em `:android-module` nem `:app`. (Não foi possível
rodar o Gradle nesta sessão — sem acesso de rede para baixar a
distribuição/dependências; validar no seu ambiente.)

### Etapa 6 — Ciclo Planner → Agente → Sessão → Validator *(iniciada nesta rodada)*
`PlanoExecucao`/`PassoPlano` (novo pacote `com.brain.planner` em `:brain`):
passo = capacidade + dependências + critério de sucesso (texto descritivo
por ora) + papel opcional do Router. `PlanoExecucao` valida ids únicos,
dependências existentes e ausência de ciclo na construção, e já expõe
`ordemDeExecucao` (ordenação topológica) — testes em
`brain/src/test/kotlin/com/brain/planner/PlanoExecucaoTest.kt`.

`CicloExecucaoPlano` (novo, em `:android-module`, pacote `com.sandbox.agent`
— precisa estar aqui porque é quem primeiro enxerga Policy/Router
(`:brain`) e Sessão/CapabilityResolver (`:android-module`) ao mesmo tempo)
percorre `ordemDeExecucao` chamando, por passo: Router (só se o passo
declarar papel) -> Policy autoriza -> `Sandbox.abrirSessao` -> capacidade
roda -> `ExecutorValidacaoProjeto` valida o workspace da sessão (novo
getter `AgentSandboxSession.workspaceHostPath`). Nega/reprova aborta o
resto do plano; dependentes ficam `BLOQUEADO_POR_DEPENDENCIA`. Testes em
`android-module/src/test/java/com/sandbox/agent/CicloExecucaoPlanoTest.kt`.

Planner que decompõe objetivo em **texto livre** para `PlanoExecucao` via
IA continua só interface (`Planner`, mesma situação do `Secretario`) — sem
isso plugado, quem monta o plano por ora é o chamador. Débito técnico
consciente registrado no KDoc de `CicloExecucaoPlano.kt`: Router só
registra a escolha (a chamada real à API do provider ainda não existe),
validação roda no host (JVM) e não dentro do proot, "corrigir e tentar de
novo" após REPROVADO não está implementado, `Decision.ASK` só é reportado
(sem espera/retomada), e os passos rodam em série (sem paralelismo entre
ramos independentes).

Critério de pronto: `./gradlew :brain:test --tests "com.brain.planner.*"`
e `./gradlew :android-module:test --tests "com.sandbox.agent.CicloExecucaoPlanoTest"`
passando. Mesma limitação da Etapa 5: sem rede nesta sessão pra baixar a
distribuição do Gradle, então a validação aqui foi por revisão manual
(traçando cada teste à mão contra a implementação) — rodar de verdade no
seu ambiente antes de considerar fechado.

### Etapa 7 — Persistência (EventStore + Memory)
Reaproveitar o padrão de persistência atômica já validado pelo SandBox
(JSON atômico com migração, usado no catálogo de plugins) para o
`EventStore` (log append-only: `JobStarted`, `PolicyChecked`,
`ValidationPassed`...) e para `Memory`. Os 112 testes Python em
`reference/braincode-python/tests/` servem de checklist de comportamento
a cobrir em Kotlin/JUnit — não de código a copiar.

## Ordem prática recomendada

1. Etapa 1 (compila) ✅
2. Etapas 2 + 3 juntas, com autorização fixa/hardcoded — primeiro fio
   de ponta a ponta rodando dentro do app.
3. Etapa 4 (capacidade real em vez de comando hardcoded).
4. Etapa 5 (Router escolhendo API).
5. Etapa 6 (Planner + ciclo completo com Validator).
6. Etapa 7 correndo em paralelo desde a Etapa 2 — cada evento já
   persistido desde o início, não só no final.

O fim da Etapa 2+3 é o ponto de virada: primeiro momento em que dá pra
dizer "o app tem um Brain de verdade preparando terreno pra um agente
trabalhar dentro do Sandbox". Tudo antes é fundação; tudo depois enriquece
o mesmo ciclo.
