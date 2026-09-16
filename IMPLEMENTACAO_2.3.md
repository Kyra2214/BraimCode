# BrainCode 2.3 — Plano de Implementação Pós-Validação

**Status:** DOCUMENTAÇÃO / NÃO IMPLEMENTAR AINDA  
**Versão:** 2.3  
**Data:** 2026-09-16  
**Repositório:** `Kyra2214/BrainCode`  
**Pré-condição:** somente iniciar depois da validação oficial e da consolidação do BrainCode atual.

---

## 0. Objetivo

A versão 2.3 não é uma reescrita do BrainCode e não deve ser aplicada enquanto o runtime atual não estiver oficialmente validado.

O objetivo é registrar, com antecedência, **o que será incorporado, de onde veio cada ideia, onde deverá entrar no BrainCode, quais contratos precisam existir, quais componentes atuais serão reutilizados e quais testes deverão provar a integração**.

A regra central é:

> **Primeiro provar o BrainCode atual. Depois incrementar.**

Nenhuma classe nova deve ser criada apenas para aumentar a arquitetura. Cada mudança da 2.3 precisa ter:

- fonte identificada;
- motivo técnico;
- ponto de integração definido;
- contrato claro;
- caller real;
- teste unitário;
- teste de integração quando aplicável;
- evidência de que o fluxo canônico realmente passa pelo componente.

---

# 1. Fontes das ideias

## 1.1 BrainCode 2.0 existente

**Fonte:** `BrainCode2.0.md` no próprio repositório.

O documento 2.0 já consolidou conceitos provenientes de vários projetos, especialmente:

- conhecimento persistente e relações;
- executor de recuperação;
- Action Gateway governado por Policy;
- skills estruturadas;
- Agent Registry;
- workflows/DAG;
- jobs duráveis;
- provider failover/cooldown.

A 2.3 deve **implementar somente depois da validação do que já está no HEAD** e deve evitar duplicar mecanismos que já existam.

---

## 1.2 CodexRouter

**Fonte principal:** `cesarfavero/codexrouter`.

O projeto apresenta uma arquitetura de gateway local capaz de gerenciar **múltiplas contas isoladas para um mesmo ponto de entrada/agente**, delegando autenticação ao cliente oficial e fazendo seleção/failover entre contas configuradas. O projeto utiliza `CODEX_HOME` separado por conta, catálogo de modelos, estado de uso/cooldown e failover entre contas configuradas. fileciteturn8file0L2-L10

A ideia que interessa ao BrainCode não é copiar o aplicativo desktop nem o código específico de Codex. É adaptar o princípio:

```text
AGENTE
  ↓
POOL DE EXECUÇÃO
  ↓
CONTAS / CREDENCIAIS ISOLADAS
  ↓
PROVIDER
  ↓
MODELO
```

O agente não deve precisar saber qual conta será usada.

O CodexRouter também demonstra uma separação importante: cada identidade recebe um ambiente isolado, enquanto o cliente oficial continua responsável pelo login, persistência e refresh de credenciais. fileciteturn8file0L2-L10

**Licença:** CodexRouter declara licença MIT e informa que adapta arquitetura/UI de outro projeto também MIT; qualquer reutilização de código deve preservar as obrigações de licença, copyright e notices correspondentes. Para BrainCode, a preferência é **adotar conceitos e contratos, não copiar implementação sem necessidade**. fileciteturn8file0L2-L10

---

# 2. Ideia central da 2.3 — Account Pool

Este é o principal incremento trazido pela análise do CodexRouter.

## 2.1 Problema

Hoje o agente não deveria ficar acoplado a uma única identidade/provider. Se uma conta atingir limite, ficar indisponível ou entrar em cooldown, o agente deve continuar através de outra identidade autorizada, quando a política permitir.

## 2.2 Modelo proposto

```text
AgentDefinition
      ↓
ProviderPolicy
      ↓
AccountPool
      ├── Account A
      ├── Account B
      ├── Account C
      └── Local Provider
              ↓
          Model Catalog
              ↓
          Execution
```

O agente pede:

```text
execute(task)
```

E não:

```text
execute(task, account="conta_X")
```

A escolha da conta pertence ao Router/Policy.

---

# 3. Componentes novos previstos

## 3.1 AccountRegistry

**Responsabilidade:** registrar identidades disponíveis para execução.

```text
AccountRegistry
├── accountId
├── providerId
├── displayName
├── status
├── credentialRef
├── capabilities
├── priority
├── cooldown
└── metadata
```

### Onde implementar

Preferencialmente no módulo Brain/runtime que já controla providers, registry e configuração. Não colocar credenciais diretamente dentro de `Agent`.

### Não fazer

- senha em texto puro;
- token em log;
- token dentro de `AgentDefinition`;
- dependência direta do agente em uma conta específica.

---

## 3.2 ProviderRegistry

Caso o BrainCode atual já possua registry de providers, **estender o existente** em vez de criar outro.

Responsabilidades:

- descobrir providers;
- associar contas;
- informar modelos disponíveis;
- expor capacidades;
- informar estado de saúde;
- fornecer adaptadores de execução.

Fluxo:

```text
ProviderRegistry
      ↓
AccountRegistry
      ↓
CapabilityDiscovery
      ↓
Model Catalog
```

---

## 3.3 Capability Catalog

A 2.3 deve aproveitar `CapabilityDiscovery` já existente.

O catálogo precisa responder algo equivalente a:

```text
qual conta pode executar esta capacidade?
qual modelo pode executar esta capacidade?
qual provider está saudável?
qual política permite seu uso?
```

Não criar um segundo mecanismo paralelo de descoberta.

---

## 3.4 AccountPool

É o coração da mudança.

```text
AccountPool
├── poolId
├── capability
├── members[]
├── selectionPolicy
├── fallbackPolicy
├── retryPolicy
└── healthPolicy
```

Exemplo conceitual:

```text
pool = coding

members:
  account-a priority=100
  account-b priority=90
  account-c priority=80
  local priority=50

fallback = enabled
cooldownOnRateLimit = enabled
maxAttempts = 3
```

O pool não deve misturar quotas de assinaturas. Ele apenas seleciona entre identidades explicitamente configuradas e autorizadas.

---

# 4. Account Router

## Responsabilidade

Selecionar a identidade de execução correta antes do TaskEngine/Provider executar uma tarefa.

Fluxo:

```text
ExecutionPlan
      ↓
Policy
      ↓
Capability
      ↓
AccountRouter
      ↓
AccountPool
      ↓
Account saudável
      ↓
Provider
      ↓
Model
      ↓
TaskEngine
```

## Regras de seleção

1. respeitar Policy;
2. respeitar capability;
3. respeitar modelo solicitado;
4. excluir conta em cooldown;
5. excluir conta indisponível;
6. respeitar prioridade;
7. selecionar somente identidade autorizada;
8. registrar a decisão;
9. permitir fallback somente quando a política permitir;
10. nunca expor credencial ao agente.

---

# 5. Health / Quota / Cooldown

O CodexRouter demonstra o valor de acompanhar estado de uso, janelas de limite, reset e cooldown localmente e realizar failover quando a conta não puder continuar. fileciteturn8file0L2-L10

No BrainCode isso deve virar um mecanismo genérico, sem depender de uma API específica.

```text
AccountHealth
├── available
├── rateLimited
├── authenticationError
├── providerError
├── quotaLow
├── quotaExhausted
├── cooldownUntil
└── lastFailure
```

### Classificação de falhas

```text
SUCCESS
RATE_LIMIT
AUTH_FAILURE
TEMPORARY_PROVIDER_FAILURE
PERMANENT_PROVIDER_FAILURE
POLICY_DENIED
INVALID_REQUEST
TIMEOUT
UNKNOWN
```

Somente falhas classificadas como recuperáveis devem provocar fallback automático.

---

# 6. Credential Isolation

A ideia de isolamento por conta do CodexRouter será adaptada para o modelo local do BrainCode. O projeto de origem usa um `CODEX_HOME` diferente para cada conta, mantendo os perfis separados. fileciteturn8file0L2-L10

No BrainCode:

```text
~/.braincode/
  accounts/
    account-a/
    account-b/
    account-c/
  agents/
  pools/
  registry/
```

O formato final dependerá do storage já utilizado pelo BrainCode.

### Regra

**Isolamento lógico obrigatório; isolamento físico quando o provider/runtime exigir.**

O agente conhece apenas `accountId` lógico ou uma referência opaca de execução. O segredo real permanece sob o Credential/Provider layer.

---

# 7. AgentDefinition

A 2.3 deve aproveitar o conceito já previsto em `BrainCode2.0.md` de Agent Registry.

```text
AgentDefinition
├── id
├── role
├── objective
├── allowedSkills
├── allowedCapabilities
├── memoryScope
├── providerPolicy
├── outputContract
└── validationPolicy
```

### Nova regra 2.3

Adicionar:

```text
providerPolicy
  ↓
accountPool / capabilityPool
```

Exemplo:

```text
agent = coder
providerPolicy = coding-pool
```

O `coder` não recebe:

```text
account = X
```

Recebe:

```text
pool = coding
```

---

# 8. Integração com as classes existentes

Este ponto é obrigatório porque a auditoria atual identificou risco de componentes novos existirem sem serem chamados pelo fluxo real.

A 2.3 **não deve repetir esse erro**.

O fluxo canônico esperado deverá ser comprovado por testes:

```text
Android / CLI / API
        ↓
Conversation
        ↓
Policy
        ↓
Planner
        ↓
AgentRegistry
        ↓
AgentDefinition
        ↓
CapabilityDiscovery
        ↓
AccountRouter
        ↓
AccountPool
        ↓
ProviderRegistry
        ↓
TaskEngine
        ↓
DurableJobRunner (quando necessário)
        ↓
Provider / Sandbox
        ↓
Result
        ↓
Critic
        ↓
Evidence / Memory
```

Se uma classe existir mas não aparecer nesse fluxo ou em uma rota deliberadamente documentada, ela deve ser considerada **não integrada**.

---

# 9. Failover sem duplicação de execução

Este é um ponto crítico.

Não basta detectar erro e tentar outra conta. O BrainCode precisa saber se a operação pode ser repetida com segurança.

```text
request
 ↓
attempt A
 ↓
resultado?
 ├─ sucesso → fim
 ├─ erro recuperável → verificar idempotência
 └─ erro não recuperável → fim
          ↓
      attempt B
```

Cada tentativa deve possuir:

```text
executionId
attemptId
accountId
providerId
modelId
startedAt
finishedAt
failureClass
```

Para ações externas com efeitos colaterais, fallback automático deve ser bloqueado ou exigir confirmação/idempotency key apropriada.

---

# 10. Catálogo de modelos

O CodexRouter sincroniza o catálogo de modelos visíveis da conta e usa o catálogo para selecionar um modelo válido por trás do gateway. fileciteturn8file0L2-L10

No BrainCode:

```text
Account
  ↓
Provider discovery
  ↓
Model Catalog
  ↓
Capability mapping
```

Exemplo:

```text
Account A
  model-1 → coding, reasoning
  model-2 → fast

Account B
  model-1 → coding
  model-3 → reasoning

Local
  model-local → coding, offline
```

O Router nunca deve selecionar um modelo apenas porque ele está escrito em configuração. Ele deve confirmar disponibilidade/capacidade quando o provider permitir descoberta.

---

# 11. Local-first e custo

A 2.3 mantém a filosofia do BrainCode:

```text
local capability
      ↓
free/authorized provider
      ↓
configured external provider
```

A existência de múltiplas contas **não deve virar mecanismo para contornar cobrança, limites contratuais ou controles de um serviço**. O pool opera apenas sobre contas/providers que o usuário está autorizado a utilizar.

O BrainCode continua sem depender de serviço remoto próprio para fazer o roteamento.

---

# 12. Doctor / Diagnóstico

O padrão de diagnóstico do CodexRouter também é útil: o projeto possui comandos para status/doctor e recuperação da integração. fileciteturn8file0L2-L10

BrainCode 2.3 deve ter diagnóstico equivalente no nível adequado ao aplicativo:

```text
braincode doctor
```

ou uma tela/comando interno que verifique:

- registry;
- providers;
- accounts;
- credentials refs;
- capabilities;
- pools;
- modelos;
- sandbox;
- jobs;
- connectivity;
- policy;
- logs seguros.

Resultado esperado:

```text
PASS AccountRegistry
PASS ProviderRegistry
PASS CapabilityDiscovery
PASS AccountPool
PASS CredentialIsolation
PASS Router
PASS Policy
PASS Sandbox
WARN Provider B cooldown
FAIL Account C authentication
```

Nunca exibir segredo no diagnóstico.

---

# 13. Observabilidade

Toda seleção deve ser explicável sem revelar credenciais.

Exemplo de evento:

```json
{
  "event": "ACCOUNT_SELECTED",
  "executionId": "...",
  "pool": "coding",
  "accountId": "account-b",
  "providerId": "provider-x",
  "reason": "priority_after_account_a_cooldown"
}
```

Também registrar:

- tentativa;
- fallback;
- cooldown;
- sucesso;
- erro classificado;
- duração;
- modelo escolhido;
- resultado da Policy.

Não registrar:

- access token;
- refresh token;
- senha;
- cookies;
- conteúdo integral de `auth.json`;
- headers secretos.

---

# 14. Testes obrigatórios

## 14.1 Unitários

### AccountRegistry

- cadastrar conta;
- remover conta;
- atualizar estado;
- persistência;
- isolamento de referência de credencial.

### AccountPool

- seleção por prioridade;
- exclusão de cooldown;
- exclusão de conta indisponível;
- pool vazio;
- múltiplas contas;
- provider local;
- capability incompatível.

### Router

- seleção correta;
- fallback;
- falha não recuperável;
- limite de tentativas;
- policy deny;
- ausência de credencial;
- modelo indisponível.

### Health

- rate limit → cooldown;
- timeout → classificação;
- auth failure → sem retry cego;
- recuperação após cooldown.

---

# 15. Teste de integração real

O teste principal da 2.3 deve provar a ideia completa:

```text
Agent
 ↓
Pool
 ↓
Account A
 ↓
falha controlada
 ↓
Account B
 ↓
Provider
 ↓
resultado
```

O teste deve provar que:

1. o agente não escolheu a conta diretamente;
2. o Router escolheu A;
3. A falhou com erro classificável;
4. Policy permitiu fallback;
5. Router selecionou B;
6. B executou;
7. o resultado foi devolvido ao agente;
8. o evento de fallback foi registrado;
9. nenhuma credencial apareceu nos logs.

---

# 16. Teste E2E Android

Depois de a integração interna estar aprovada:

```text
Android UI
 ↓
chat
 ↓
BrainApiGateway
 ↓
Brain runtime
 ↓
Agent
 ↓
AccountRouter
 ↓
Provider
 ↓
resultado
 ↓
Android UI
```

Este E2E é particularmente importante porque a documentação atual do BrainCode já identifica a necessidade de provar o caminho Android → gateway.

A 2.3 não deve ser considerada integrada enquanto esse caminho não estiver demonstrado por execução real.

---

# 17. Compatibilidade com DurableJobRunner

Não transformar todo request em Job.

```text
curto/síncrono
 → execução direta

longo/assíncrono
 → DurableJobRunner
```

Quando houver job durável, o `accountId`, `providerId`, `modelId` e estado da tentativa precisam ser persistidos de forma segura para que uma reinicialização não destrua o estado da execução.

---

# 18. Compatibilidade com Sandbox

Account Pool não substitui Sandbox.

A separação correta continua sendo:

```text
Policy
 ↓
Capability
 ↓
Action
 ↓
Sandbox
```

O provider escolhido pode executar uma tarefa dentro do Sandbox, mas não recebe autorização adicional só porque foi escolhido pelo Router.

---

# 19. Compatibilidade com Critic

O Critic continua sendo pós-execução.

```text
Provider
 ↓
resultado
 ↓
Critic
 ↓
validado / rejeitado / revisão
```

Uma resposta obtida após failover não recebe confiança maior ou menor apenas por ter vindo de outra conta.

A avaliação continua baseada em evidência, contrato e política.

---

# 20. O que NÃO será levado do CodexRouter

Não implementar no BrainCode apenas por existir no projeto fonte:

- Electron;
- React/Vite desktop launcher;
- menu bar macOS;
- integração específica com Codex;
- catálogo com nomes específicos de modelos;
- `CODEX_HOME` como requisito universal;
- endpoints específicos do Codex;
- UI de desktop;
- login proprietário;
- automação de navegador;
- importação de cookies;
- mecanismo de assinatura/quota específico de outro serviço.

Apenas os padrões arquiteturais compatíveis com o BrainCode serão adaptados.

---

# 21. Ordem de implementação

A implementação futura deve ocorrer nesta ordem:

### Fase 0 — Gate

```text
VALIDAÇÃO OFICIAL ATUAL
        ↓
PASS
        ↓
congelar baseline
```

Se falhar: **não iniciar 2.3**.

### Fase 1 — Contratos

Criar/validar:

- Account;
- AccountRegistry;
- Provider;
- ProviderRegistry;
- AccountPool;
- AccountHealth;
- SelectionPolicy;
- ExecutionAttempt.

### Fase 2 — Registry

Integrar registry de contas com provider/capability existente.

### Fase 3 — Router

Integrar AccountRouter ao fluxo canônico.

### Fase 4 — Failover

Implementar classificação de erro, cooldown e retry seguro.

### Fase 5 — Observabilidade

Adicionar eventos e diagnóstico sem segredos.

### Fase 6 — E2E

Provar Brain runtime e posteriormente Android → gateway.

### Fase 7 — Consolidação

Remover classes/abstrações duplicadas e atualizar documentação.

---

# 22. Critério de aceite da 2.3

A 2.3 somente poderá ser marcada como concluída se todos forem verdadeiros:

- [ ] baseline atual aprovado;
- [ ] AccountRegistry integrado;
- [ ] ProviderRegistry reutilizado/estendido;
- [ ] CapabilityDiscovery integrado;
- [ ] AccountPool integrado;
- [ ] Agent não escolhe credencial diretamente;
- [ ] Router seleciona conta;
- [ ] Policy continua sendo autoridade;
- [ ] fallback possui classificação de erro;
- [ ] cooldown funciona;
- [ ] retries possuem limite;
- [ ] ações não idempotentes não sofrem retry cego;
- [ ] credenciais ficam isoladas;
- [ ] logs não vazam segredos;
- [ ] catálogo de modelos é validado quando possível;
- [ ] DurableJobRunner preserva estado quando necessário;
- [ ] Critic continua no fluxo;
- [ ] testes unitários passam;
- [ ] integração multi-conta passa;
- [ ] E2E passa;
- [ ] Android → gateway passa quando aplicável;
- [ ] build/release passa;
- [ ] nenhuma classe nova ficou órfã.

---

# 23. Relação com o BrainCode 2.0

O `BrainCode2.0.md` continua sendo o documento amplo de consolidação arquitetural.

Este `IMPLEMENTACAO_2.3.md` é deliberadamente diferente:

```text
BrainCode2.0.md
  = O QUE VALE A PENA ADOTAR

IMPLEMENTACAO_2.3.md
  = COMO / ONDE / EM QUE ORDEM IMPLEMENTAR
```

A 2.3 também incorpora o novo aprendizado do CodexRouter:

```text
Agent
 ↓
Policy / Capability
 ↓
AccountPool
 ↓
Provider
 ↓
Model
```

com contas isoladas, health/cooldown, failover e observabilidade.

---

# 24. Regra final de governança

**NÃO IMPLEMENTAR AGORA.**

Este documento é um plano de incremento posterior.

O estado desejado é:

```text
                 AGORA
                   │
                   ▼
        ┌─────────────────────┐
        │ Consolidar BrainCode│
        └──────────┬──────────┘
                   ▼
        ┌─────────────────────┐
        │ Validação oficial   │
        └──────────┬──────────┘
                   │
             PASS? │
              ┌────┴────┐
             NÃO       SIM
              │          │
              ▼          ▼
          corrigir    congelar
                       baseline
                          │
                          ▼
                    IMPLEMENTAÇÃO
                         2.3
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
         Accounts       Router       Failover
             │            │            │
             └────────────┼────────────┘
                          ▼
                    TESTES / E2E
                          │
                          ▼
                       RELEASE
```

A regra é simples: **não adicionar complexidade antes de provar que a base funciona.**

---

## Fontes principais

1. `Kyra2214/BrainCode` — `BrainCode2.0.md`, arquitetura e gaps já documentados no próprio projeto. fileciteturn7file0L2-L2
2. `cesarfavero/codexrouter` — README, arquitetura de gateway, contas isoladas, catálogo, usage/cooldown, failover, E2E, segurança e licença MIT. fileciteturn8file0L2-L10

**Observação de licenciamento:** a implementação do BrainCode deve preferir reprodução de conceitos/contratos. Se algum trecho de código do projeto fonte for reutilizado futuramente, revisar e preservar integralmente as obrigações da licença e os notices de terceiros aplicáveis.