# BRAINCODE 2.0 — PLANO MESTRE DE CONSOLIDAÇÃO

## 1. Objetivo

O BrainCode 2.0 não deve ser apenas um chatbot que chama APIs, nem um conjunto de agentes autônomos. O objetivo é construir um sistema operacional de execução inteligente no qual o Brain entende o objetivo, descobre quais capacidades existem, monta o caminho mínimo necessário, aplica política, executa por meio do Sandbox, valida o resultado e transforma aprendizados reutilizáveis em conhecimento, skills ou workflows.

Arquitetura-alvo:

```text
Chat → Brain → Memory → Intent/Plan → Capability Discovery
     → Agent/Skill/Tool → Policy → Action Gateway
     → Sandbox/Provider/Jobs → Evidence → Critic
     → Memory/Knowledge Compiler → Chat
```

Princípio central:

> Brain decide. Registry descobre. Policy autoriza. Gateway executa. Sandbox protege. Evidence registra. Critic valida. Memory aprende.

---

## 2. Fontes consolidadas

O plano combina quatro linhas de trabalho:

### 2.1 Arquitetura BrainCode

- Brain como orquestrador.
- Chat como interface do usuário.
- Memory/Knowledge como camada de conhecimento.
- Critic como validação.
- Sandbox como fronteira de execução.
- APIs como capacidades externas, não como cérebro.
- Agentes amarrados, sem LLM próprio e com missão/capacidades delimitadas.
- Descoberta dinâmica de providers/modelos.
- Política free-first.

### 2.2 Plano do Claude

- Classificação determinística barata.
- Search Agent.
- Library/Knowledge Agent.
- Function Splitter.
- Planner.
- Dispatcher.
- Prompt Library.
- Execução em etapas para tarefas complexas.

### 2.3 Repositório/open-source analisado

Conceitos aproveitados:

- conhecimento persistente e manutenção de conhecimento;
- skills reutilizáveis e declarativas;
- catálogo de agentes;
- workflows/DAG;
- jobs assíncronos e duráveis;
- governança, aprovação e auditoria;
- contexto e memória persistentes.

Projetos de referência analisados incluem LLM Wiki Agent, Open Grok Bot, Generative Media Skills, Vibe Workflow, Open AI Agents Hub, Open Claude Tag e PixelRelay.

### 2.4 IaBrain trazido pelo Manus

O IaBrain não será copiado nem integrado diretamente. Serão reaproveitados os conceitos mais fortes:

- `IACapabilityRegistry` como inspiração para um Registry universal de capacidades;
- candidatos de roteamento com qualidade, velocidade, custo, confiabilidade e contexto;
- catálogos declarativos;
- separação entre comando, capacidade e executor;
- `ExecutionSecurityPolicy` como referência para o Policy Broker;
- resolução baseada em capacidades em vez de dependência exclusiva de nomes de modelos.

---

# 3. Regra de consolidação

Antes de criar qualquer componente novo:

1. verificar se o BrainCode já possui a função;
2. se existir, evoluir;
3. se houver duas implementações, consolidar;
4. se existir parcialmente, generalizar;
5. somente se não existir, criar.

Não devem ser criadas arquiteturas paralelas apenas para reproduzir uma função já existente.

---

# 4. Arquitetura final

```text
                         BRAINCODE
                            │
             ┌──────────────┴──────────────┐
             │                             │
           CHAT                          MEMORY
             │                             │
             └──────────────┬──────────────┘
                            BRAIN
                              │
                     ┌────────┴────────┐
                     │                 │
                 CLASSIFIER         PLANNER
                     │                 │
                     └────────┬────────┘
                              │
                    CAPABILITY DISCOVERY
                              │
             ┌────────────────┼────────────────┐
             │                │                │
           AGENT            SKILL             TOOL
             │                │                │
             └────────────────┼────────────────┘
                              │
                         POLICY BROKER
                              │
                        ACTION GATEWAY
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
       SANDBOX              API                JOBS
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                         DISPATCHER
                              │
                           EVIDENCE
                              │
                            CRITIC
                              │
                    KNOWLEDGE COMPILER
                              │
                    ┌─────────┼─────────┐
                    │         │         │
                 MEMORY     SKILLS    WORKFLOWS
```

---

# 5. Componentes e responsabilidades

| Componente | Responsabilidade |
|---|---|
| Chat | Conversar com o usuário |
| Brain | Orquestrar e decidir |
| Classifier | Classificação rápida e barata |
| Memory | Recuperar conhecimento validado |
| Planner | Criar planos para tarefas complexas |
| Function Splitter | Dividir objetivos em tarefas |
| Capability Registry | Registrar capacidades e fornecedores |
| Capability Discovery | Encontrar capacidades adequadas |
| Agent Registry | Registrar agentes amarrados |
| Agent | Executar missão delimitada |
| Skill Registry | Registrar procedimentos reutilizáveis |
| Tool Registry | Registrar operações concretas |
| Command Registry | Representar comandos/intenções declarativas |
| Policy Broker | Autorizar, limitar, exigir aprovação ou bloquear |
| Action Gateway | Ponto único de entrada para execução |
| Dispatcher | Encaminhar tarefas para executores |
| Sandbox | Ambiente de execução isolado |
| Provider Router | Selecionar provider/modelo disponível |
| Workflow Engine | Executar planos/DAGs |
| Job Store | Persistir tarefas longas |
| Evidence | Resultado + proveniência |
| Critic | Validar resultado |
| Knowledge Compiler | Converter experiências validadas em conhecimento reutilizável |

---

# 6. Universal Capability Model

A principal generalização extraída do IaBrain é substituir um registro exclusivo de IAs por um registro universal.

Uma capacidade pode ser fornecida por:

- API;
- Agent;
- Skill;
- Tool;
- Sandbox;
- Command;
- Provider;
- Workflow.

Modelo conceitual:

```kotlin
data class CapabilityDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: CapabilityCategory,
    val inputSchema: String?,
    val outputSchema: String?,
    val providerIds: List<String>,
    val requiredPermissions: Set<Permission>,
    val riskLevel: RiskLevel,
    val networkRequired: Boolean,
    val sandboxRequired: Boolean,
    val deterministic: Boolean,
    val supportsAsync: Boolean,
    val costClass: CostClass,
    val reliability: Double,
    val quality: Double,
    val speed: Double
)
```

O modelo é conceitual neste documento; a implementação deve ser adaptada às estruturas já existentes no BrainCode.

---

# 7. Capability Registry

O Registry deve oferecer, conceitualmente:

```text
register()
remove()
update()
getById()
findByCategory()
findByCapability()
findByRisk()
findByCost()
findByProvider()
discover(requirements)
```

Exemplo:

```text
Brain: "Preciso pesquisar GitHub e verificar código."

Discovery:
GITHUB_READ
CODE_TEST
FILE_READ

Candidatos:
GitHub Tool
ResearchAgent
CodeAgent
SandboxTest
```

O Brain não deve ficar preso a uma implementação específica.

---

# 8. Capability Discovery

A descoberta deve ser hierárquica e sob demanda.

Não carregar centenas de ferramentas no contexto do Brain sem necessidade.

Exemplo:

```text
CODE
 ├── READ
 ├── WRITE
 ├── TEST
 ├── BUILD
 ├── DEBUG
 └── REVIEW
```

O Brain seleciona primeiro a capacidade necessária e só depois resolve a implementação.

---

# 9. Fast Intent Classifier

O `KeywordSecretario`/classificador do plano do Claude deve ser tratado como classificador rápido, não como cérebro.

Exemplos:

```text
"Oi"                         → CHAT
"Explique Gradle"            → EXPLAIN
"Procure no GitHub"           → RESEARCH
"Corrija este código"         → CODE
"Crie um aplicativo completo"→ PLAN/BUILD
```

O classificador retorna intenção e confiança. Ele não escolhe sozinho Agent, API ou executor.

---

# 10. Memory-first

Antes de usar API ou LLM, o Brain deve consultar conhecimento validado.

Fluxo preferencial:

```text
Chat
 ↓
Brain
 ↓
Memory
 ↓
FOUND → resposta
NOT FOUND → Discovery/Execution
```

Conhecimento reutilizável deve conter não apenas a resposta, mas também como e onde a solução foi encontrada.

---

# 11. Evidence, Knowledge e Skill

Três níveis devem permanecer separados.

### Evidence

Resultado bruto com fonte/proveniência:

```text
URL
GitHub
arquivo
commit
log
resposta de API
```

### Knowledge Candidate

Hipótese derivada da evidência, ainda não validada.

### Validated Knowledge

Conhecimento confirmado por teste, reprodução, fonte confiável ou múltiplas confirmações.

Uma experiência que descreve um procedimento pode virar Skill após validação.

---

# 12. Retrieval Hints

Toda entrada relevante deve poder registrar como a solução foi recuperada.

Exemplo:

```text
Problema:
Java não encontrado no RootFS.

Solução:
Executar o probe com JAVA_TOOL_OPTIONS apropriado.

Origem:
BrainCode / arquivo / commit

Caminho de recuperação:
RootFS → probe → Java → low-memory
```

Isso transforma Memory em uma memória de soluções e caminhos de descoberta, não apenas em um FAQ.

---

# 13. Research Agent

O Search Agent do Claude será consolidado como `ResearchAgent`.

Características:

- bounded;
- sem LLM próprio;
- recebe missão explícita;
- recebe capabilities permitidas;
- respeita Policy;
- executa no Sandbox quando necessário;
- retorna Evidence e Sources.

Exemplo:

```text
Mission:
Encontrar documentação oficial sobre X.

Capabilities:
WEB_SEARCH
GITHUB_READ
FILE_READ
```

O agente termina após cumprir a missão; não redefine o objetivo do usuário.

---

# 14. Knowledge/Skill Retrieval Agent

O Library Agent do Claude será generalizado para recuperação de:

- Memory;
- Knowledge;
- Prompt Library;
- Skills;
- Workflows.

Estados possíveis:

```text
FOUND
NOT_FOUND
CANDIDATE
```

---

# 15. Agent Registry

Agentes devem declarar:

- id;
- capabilities;
- inputs;
- outputs;
- risk;
- requisitos de Sandbox;
- executor.

Exemplos iniciais:

```text
ResearchAgent
CodeAgent
ValidationAgent
KnowledgeAgent
PlanningAgent
```

Todos continuam amarrados e delimitados.

---

# 16. Skill Registry

Skill não é Agent.

Skill é conhecimento/procedimento reutilizável.

Exemplos:

```text
android-build-diagnosis
android-rootfs-diagnosis
github-research
code-review
debug-kotlin
project-audit
```

Uma Skill deve poder declarar:

```text
id
version
description
trigger
inputs
outputs
requiredCapabilities
procedure
validation
risk
source
confidence
status
```

Status:

```text
CANDIDATE
TESTING
VALIDATED
DEPRECATED
BLOCKED
```

---

# 17. Prompt Library

A Prompt Library não será apenas uma coleção de textos.

Ela será uma biblioteca de procedimentos cognitivos reutilizáveis.

Exemplos:

```text
code-review
debug-kotlin
android-build-failure
github-research
architecture-audit
security-review
data-analysis
```

Prompts/skills devem ter contexto, entradas, saídas, capacidades necessárias e critérios de sucesso quando aplicável.

---

# 18. Policy Broker

O `ExecutionSecurityPolicy` do IaBrain será tratado como referência, não copiado literalmente.

O Policy Broker deverá considerar:

```text
quem
→ quer fazer o quê
→ com quais dados
→ usando qual capability
→ em qual ambiente
→ com qual risco
```

Decisões:

```text
ALLOW
ALLOW_WITH_LIMITS
REQUIRE_APPROVAL
DENY
```

Exemplos:

```text
FILE_READ  → ALLOW
FILE_WRITE → depende do escopo
SECRET      → DENY ou fluxo especial
BUILD      → ALLOW em Sandbox
```

---

# 19. Action Gateway

Toda execução deve passar por uma porta uniforme:

```text
Brain
 ↓
Policy
 ↓
ActionGateway
 ↓
Executor
```

O Gateway pode encaminhar para:

- Agent;
- Tool;
- Skill;
- API;
- Sandbox;
- Workflow;
- Job.

Ele deve padronizar request, autorização, execução, resultado, Evidence e auditoria.

---

# 20. Action Lifecycle

Estados sugeridos:

```text
CREATED
PLANNED
AUTHORIZED
DISPATCHED
RUNNING
SUCCEEDED
FAILED
BLOCKED
RETRYING
CANCELLED
```

Cada ação deve produzir evento auditável.

---

# 21. Audit Event

Modelo conceitual:

```text
actionId
timestamp
actor
capability
agent
inputHash
policyDecision
executor
status
duration
outputReference
evidence
```

Não é necessário armazenar conteúdo bruto em todos os eventos; referências e hashes podem ser usados quando apropriado.

---

# 22. Dispatcher

O Dispatcher não raciocina sobre o objetivo.

Ele recebe uma tarefa já definida e encontra o executor adequado.

Exemplos:

```text
CODE_TEST   → Sandbox Test Executor
GITHUB_READ → GitHub Tool
WEB_SEARCH  → Research Executor
LLM_REASONING → Provider Gateway
```

A decisão de qual capacidade é necessária permanece com Brain/Planner/Discovery.

---

# 23. Function Splitter

Objetivos compostos devem ser divididos em tarefas.

Exemplo:

```text
"Audite, corrija, teste e entregue o projeto."

T1 ler projeto
T2 analisar arquitetura
T3 identificar problemas
T4 propor correções
T5 implementar
T6 executar testes
T7 validar
T8 gerar resultado
```

---

# 24. ExecutionPlan

Modelo conceitual:

```text
ExecutionPlan
 ├── goal
 ├── assumptions
 ├── tasks
 ├── dependencies
 ├── requiredCapabilities
 ├── policies
 ├── successCriteria
 └── fallback
```

Cada Task:

```text
id
objective
inputs
outputs
dependencies
capabilities
agent
retryPolicy
validation
```

---

# 25. Planner

Planner é reservado para tarefas complexas.

Tarefa simples não deve pagar o custo de planejamento desnecessário.

O Planner produz ExecutionPlan; não executa diretamente.

Fluxo:

```text
Brain
 ↓
Planner
 ↓
ExecutionPlan
 ↓
Capability Discovery
 ↓
Policy
 ↓
Dispatcher
```

---

# 26. Workflow/DAG Engine

Quando tarefas têm dependências:

```text
A
├── B
└── C
     ↓
     D
```

Pode executar:

```text
A
 ↓
B + C
 ↓
D
```

O Workflow Engine executa o plano; não substitui o Brain como decisor.

---

# 27. APIs como capabilities

Não deve existir uma arquitetura em que cada tarefa aponta diretamente para uma API.

O fluxo correto é:

```text
Task
 ↓
Capability Discovery
 ↓
Capability
 ↓
Provider
```

Isso permite trocar API A por API B, Agent, Sandbox ou outra implementação sem alterar a intenção original.

---

# 28. Provider Router

Modelos e providers continuam sendo descobertos dinamicamente.

Fluxo:

```text
Provider
 ↓
Catalog
 ↓
Available Models
 ↓
Capabilities
 ↓
Health
 ↓
Routing
```

Não manter uma lista fixa de modelos como autoridade definitiva.

---

# 29. Critério de seleção de candidatos

A ideia do `IACapabilityRegistry` será generalizada.

Um candidato pode ter:

```text
capabilityMatch
quality
reliability
speed
cost
contextFit
policyFit
availability
```

A Policy pode eliminar um candidato antes do cálculo de score.

O score nunca deve permitir executar algo que a Policy bloqueou.

---

# 30. Free-first

A política econômica continua:

```text
FREE_PERMANENT
FREE_TIER
```

preferidos.

Créditos promocionais temporários e PAYG não devem ser tratados como gratuito permanente.

LLM é usado somente quando a tarefa realmente requer raciocínio ou geração não determinística.

---

# 31. Quando usar LLM

LLM é uma capability, não o cérebro inteiro.

Pode ser usado para:

- reasoning;
- planning;
- code generation;
- summarization;
- classificação difícil;
- prompt generation;
- análise.

Não deve ser usado para tarefas que podem ser determinísticas:

- leitura simples de arquivo;
- checksum;
- consulta de catálogo;
- regex;
- cópia de arquivo;
- execução de teste;
- validações estruturais simples.

---

# 32. Knowledge Compiler

Após uma execução:

```text
Evidence
 ↓
Critic
 ↓
Knowledge Compiler
```

O Compiler avalia se a experiência é reutilizável.

Pode produzir:

```text
Knowledge
Skill
Prompt
Workflow
Policy/Rule
```

quando houver justificativa e validação.

---

# 33. Skill Validation

Nunca:

```text
LLM criou skill
 ↓
salvar como verdade
```

Sempre que possível:

```text
Candidate Skill
 ↓
Sandbox
 ↓
Test
 ↓
Critic
 ↓
PASS
 ↓
Validated Skill
```

---

# 34. Durable Job Store

Tarefas longas não devem depender exclusivamente de memória de processo.

Modelo conceitual:

```text
Job
 ├── id
 ├── status
 ├── createdAt
 ├── updatedAt
 ├── plan
 ├── currentTask
 ├── retryCount
 ├── result
 └── error
```

Estados:

```text
QUEUED
RUNNING
PAUSED
WAITING_APPROVAL
FAILED
COMPLETED
CANCELLED
```

Se o processo Android reiniciar, o job deve poder ser retomado quando tecnicamente possível.

---

# 35. Exemplos de fluxo

## 35.1 Pergunta simples

```text
Chat
 ↓
Brain
 ↓
Memory
 ↓
FOUND
 ↓
Chat
```

Sem API desnecessária.

## 35.2 Pesquisa

```text
Chat
 ↓
Brain
 ↓
Memory
 ↓
Research Intent
 ↓
Capability Discovery
 ↓
GITHUB_READ + WEB_SEARCH
 ↓
ResearchAgent
 ↓
Sandbox
 ↓
Evidence
 ↓
Critic
 ↓
Memory
 ↓
Chat
```

## 35.3 Correção de código

```text
Chat
 ↓
Brain
 ↓
Memory
 ↓
Skill/Capability Discovery
 ↓
FILE_READ + CODE_GENERATION + CODE_TEST
 ↓
CodeAgent
 ↓
Sandbox
 ↓
Tests
 ↓
Evidence
 ↓
Critic
 ↓
Memory
 ↓
Chat
```

## 35.4 Projeto complexo

```text
Chat
 ↓
Brain
 ↓
Memory
 ↓
Complex Task
 ↓
Planner
 ↓
ExecutionPlan
 ↓
Function Splitter
 ↓
Capability Discovery
 ↓
Policy
 ↓
Dispatcher
 ↓
Agents/Tools/Sandbox
 ↓
Evidence
 ↓
Critic
 ↓
Retry/Correction/Fallback
 ↓
Knowledge Compiler
 ↓
Memory
 ↓
Chat
```

---

# 36. Exemplo de execução completa

Usuário:

> Descubra por que o Java não foi encontrado nos RootFS, corrija o BrainCode e valide.

Fluxo esperado:

```text
Intent = PROJECT_DEBUG

Memory:
procura conhecimento de Java/RootFS

Capability Discovery:
FILE_READ
GITHUB_READ
CODE_ANALYSIS
CODE_EDIT
CODE_TEST
ROOTFS_INSPECTION

Agents:
ResearchAgent
CodeAgent

Planner:
T1 identificar fluxo RootFS
T2 analisar arquivos
T3 reproduzir probe Java
T4 identificar causa
T5 corrigir
T6 build
T7 testar
T8 verificar regressão

Policy:
FILE_READ → ALLOW
CODE_EDIT → conforme escopo
BUILD → ALLOW no Sandbox

Dispatcher:
Tarefas encaminhadas aos executores

Evidence:
logs + arquivos + commit + testes

Critic:
PASS/CORRECT/RETRY/FALLBACK/ABORT

Knowledge Compiler:
atualiza conhecimento RootFS/Java se a solução for reutilizável e validada
```

---

# 37. Fases de implementação

## FASE 0 — CONSOLIDAÇÃO

Auditar o BrainCode atual:

- Brain;
- Chat;
- Memory;
- Knowledge;
- Critic;
- API Gateway;
- Router;
- Sandbox;
- Agents;
- PromptLibrary;
- Execution;
- Security;
- RootFS.

Produzir mapa:

```text
EXISTE
PARCIAL
DUPLICADO
FALTA
SUBSTITUIR
```

Não implementar nova arquitetura antes desta consolidação.

---

## FASE 1 — UNIVERSAL CAPABILITY MODEL

Criar/consolidar:

```text
CapabilityDefinition
CapabilityProvider
CapabilityRegistry
CapabilityCandidate
CapabilityDiscovery
```

Teste fundamental:

> Quem consegue executar GITHUB_READ?

O Registry deve retornar todos os candidatos elegíveis.

---

## FASE 2 — POLICY BROKER

Evoluir os conceitos de segurança existentes para:

```text
ALLOW
ALLOW_WITH_LIMITS
REQUIRE_APPROVAL
DENY
```

Adicionar testes de escopo, dados sensíveis, capacidades, Sandbox e risco.

---

## FASE 3 — ACTION GATEWAY

Unificar execução de:

```text
Agent
Tool
API
Skill
Sandbox
Workflow
Job
```

Nenhum executor deve contornar Policy e Gateway sem justificativa arquitetural explícita.

---

## FASE 4 — AGENT REGISTRY

Migrar os agentes amarrados atuais para registro declarativo.

Primeiros agentes:

```text
ResearchAgent
CodeAgent
```

Expandir somente quando houver necessidade real.

---

## FASE 5 — SKILL REGISTRY

Criar registro de skills reutilizáveis.

Primeiras candidatas:

```text
research
github-research
code-test
android-build
project-audit
debug
```

---

## FASE 6 — RETRIEVAL EXECUTOR

Implementar recuperação integrada de:

```text
Memory
Knowledge
Skill
Prompt
Workflow
```

antes de chamar API quando houver solução validada.

---

## FASE 7 — KNOWLEDGE COMPILER

Transformar evidências e execuções validadas em conhecimento reutilizável.

Saídas possíveis:

```text
Knowledge
Skill
Prompt
Workflow
Rule
```

---

## FASE 8 — PLANNER + FUNCTION SPLITTER

Criar:

```text
Intent
ExecutionPlan
Task
Dependency
FunctionSplitter
Planner
```

Somente para tarefas que realmente precisam de decomposição.

---

## FASE 9 — DISPATCHER

O Dispatcher recebe Tasks e encaminha para o Action Gateway.

---

## FASE 10 — WORKFLOW/DAG

Adicionar:

- dependências;
- paralelismo seguro;
- retry;
- falha;
- recuperação;
- critérios de conclusão.

---

## FASE 11 — DURABLE JOBS

Adicionar JobStore para tarefas longas e retomáveis.

---

## FASE 12 — AUTO-SKILL LEARNING

Detectar padrões recorrentes de execuções bem-sucedidas e gerar candidatos de Skill.

Sempre validar antes de promover para `VALIDATED`.

---

## FASE 13 — CAPABILITY DISCOVERY HIERÁRQUICO

Quando o catálogo crescer, adotar descoberta por categorias e carregamento sob demanda.

---

## FASE 14 — OBSERVABILIDADE

Criar visualização técnica de:

```text
Task
 ↓
Plan
 ↓
Capabilities
 ↓
Policy
 ↓
Agent
 ↓
Sandbox
 ↓
Evidence
 ↓
Critic
```

Isso será especialmente importante para depuração e auditoria.

---

# 38. Ordem oficial de implementação

A ordem recomendada é:

```text
CONSOLIDAR
   ↓
CAPABILITY MODEL
   ↓
CAPABILITY REGISTRY
   ↓
CAPABILITY DISCOVERY
   ↓
POLICY BROKER
   ↓
ACTION GATEWAY
   ↓
AGENT REGISTRY
   ↓
SKILL REGISTRY
   ↓
RETRIEVAL
   ↓
KNOWLEDGE COMPILER
   ↓
PLANNER
   ↓
FUNCTION SPLITTER
   ↓
DISPATCHER
   ↓
WORKFLOW/DAG
   ↓
JOB STORE
   ↓
AUTO-SKILLS
```

Não começar pelo Planner. O Planner depende de uma infraestrutura de capacidades e execução bem definida.

---

# 39. O que não entra no BrainCode 2.0

Explicitamente fora do escopo:

- LLM local baixado no aplicativo;
- engine local de inferência;
- download automático de modelos;
- Agent autônomo com LLM próprio;
- lista fixa de modelos como autoridade;
- API obrigatória para toda tarefa;
- slash commands como cérebro do sistema;
- cópia integral do Room/schema do IaBrain;
- cópia literal de projetos externos;
- conhecimento não validado promovido automaticamente;
- duplicação de componentes já existentes.

---

# 40. Regras de arquitetura

### Regra 1
O usuário conversa somente com Chat.

### Regra 2
Chat conversa com Brain.

### Regra 3
Brain é o orquestrador central.

### Regra 4
Agents são executores amarrados.

### Regra 5
Capabilities são abstrações; implementações podem variar.

### Regra 6
Policy pode bloquear qualquer ação antes da execução.

### Regra 7
Toda execução relevante deve produzir Evidence.

### Regra 8
Critic decide se Evidence é suficiente para promover conhecimento.

### Regra 9
Memory é consultada antes de gastar recursos externos sempre que aplicável.

### Regra 10
LLM é capability, não autoridade absoluta.

### Regra 11
O Sandbox é a fronteira de execução.

### Regra 12
Nenhum projeto externo deve ser copiado sem adaptar ao modelo do BrainCode.

### Regra 13
Toda nova infraestrutura deve primeiro verificar duplicação com o código existente.

---

# 41. Visão final

O BrainCode 2.0 deverá funcionar como um sistema operacional de capacidades:

```text
                    ┌───────────────┐
                    │      CHAT     │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │     BRAIN     │
                    └───────┬───────┘
                            │
                 ┌──────────▼──────────┐
                 │ MEMORY / KNOWLEDGE  │
                 └──────────┬──────────┘
                            │
                 ┌──────────▼──────────┐
                 │ INTENT / EXECUTION  │
                 │       PLAN         │
                 └──────────┬──────────┘
                            │
                 ┌──────────▼──────────┐
                 │ CAPABILITY DISCOVERY│
                 └──────────┬──────────┘
                            │
               ┌────────────┼────────────┐
               │            │            │
            AGENTS        SKILLS       TOOLS
               │            │            │
               └────────────┼────────────┘
                            │
                    ┌───────▼───────┐
                    │     POLICY    │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │ ACTION GATEWAY│
                    └───────┬───────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
          SANDBOX          API            JOB
             │              │              │
             └──────────────┼──────────────┘
                            │
                    ┌───────▼───────┐
                    │    EVIDENCE   │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │     CRITIC    │
                    └───────┬───────┘
                            │
                 ┌──────────▼──────────┐
                 │ KNOWLEDGE COMPILER  │
                 └──────────┬──────────┘
                            │
                    MEMORY / SKILLS
```

## Síntese

O BrainCode não deve ser uma coleção de agentes inteligentes independentes.

Deve ser um sistema com **um cérebro de orquestração e múltiplos executores controlados por capacidades, política e evidência**.

A grande generalização do IaBrain é o `Capability Registry` universal.

A principal contribuição do plano do Claude é o pipeline `Classifier → Planner → Function Splitter → Dispatcher` para tarefas progressivamente mais complexas.

A principal contribuição dos projetos externos é a transformação de experiências em conhecimento, skills, workflows, jobs e trilhas de auditoria reutilizáveis.

A arquitetura resultante mantém o BrainCode local-first, free-first, seguro, auditável, modular e capaz de evoluir sem depender de um único modelo, provider ou agente.

> **Brain decide. Registry descobre. Policy autoriza. Gateway executa. Sandbox protege. Evidence registra. Critic valida. Memory aprende.**
