# BrainCode — Arquitetura Atual

**Fonte canônica da arquitetura.** Atualizada após a consolidação do BrainCode 2.0.

## 1. Responsabilidades

| Componente | Responsabilidade | Não deve fazer |
|---|---|---|
| Chat | Conversar com o usuário | Escolher provider ou executar comando bruto |
| Brain | Interpretar, decidir, planejar e orquestrar | Executar shell arbitrário |
| Memory | Recuperar conhecimento validado | Promover candidato sem validação |
| Capability Registry | Registrar capacidades disponíveis | Executar capacidades |
| Capability Discovery | Encontrar candidatos adequados | Autorizar execução |
| PolicyBroker | Decidir se uma ação pode ocorrer | Ser executor |
| ActionGateway | Abrir a fronteira controlada de execução | Aceitar comando bruto |
| Agent | Executar uma missão delimitada | Criar objetivo próprio ou possuir LLM próprio |
| Skill | Encapsular procedimento reutilizável | Bypassar Policy/Gateway |
| Tool/API | Fornecer capacidade específica | Tornar-se o cérebro |
| Sandbox | Isolar/controlar execução | Decidir o objetivo do usuário |
| Evidence/Event | Registrar resultado e proveniência | Ser tratado automaticamente como verdade |
| Critic | Validar resultado | Substituir o executor |

## 2. Modelo universal de capacidade

A unidade comum de integração é `CapabilityDefinition` em `:brain`.

Uma capability descreve, entre outros atributos:

- identidade e descrição;
- categoria;
- owner/provedor;
- parâmetros e schema;
- capacidades requeridas/fornecidas;
- permissões e risco;
- custo/latência;
- disponibilidade/status;
- suporte a código, arquivos, web e raciocínio;
- confiabilidade;
- proveniência e versão.

Uma capability pode ser fornecida por API, provider, Agent, Skill, Tool, Command, Sandbox, Workflow ou componente interno.

## 3. Descoberta

```text
CapabilityRegistry
       ↓
CapabilityDiscovery
       ↓
CapabilityCandidate(score, reasons)
```

O discovery compara requisitos da tarefa com capacidades disponíveis. Qualidade, custo, latência, disponibilidade e contexto são fatores de seleção, não ordens absolutas.

## 4. Autorização

```text
Intent/Plan
   ↓
CapabilityCandidate
   ↓
PolicyBroker
   ├── ALLOW
   ├── DENY
   ├── REQUIRE_APPROVAL
   └── SANDBOX_ONLY
```

A descoberta nunca equivale a autorização.

## 5. Execução

```text
Policy decision
      ↓
ActionGateway
      ↓
ActionExecutor
      ↓
Sandbox / Provider / Agent / Tool
      ↓
Evidence + Audit Event
```

A ação deve possuir identidade (`actionId`), actor, capability, parâmetros controlados, decisão de policy, timestamps, resultado/erro e proveniência/evidência quando disponível.

## 6. Agents bounded

Um Agent é um trabalhador especializado, não um segundo cérebro.

```text
Mission
 + AllowedCapabilities
 + Policy
 + Sandbox
       ↓
BoundedAgent
       ↓
Evidence
```

O Agent não baixa LLM, não recebe objetivo autônomo e não pode ultrapassar as capabilities autorizadas.

## 7. Memória e aprendizado

```text
Memory recall
   ↓ não encontrou
External capability
   ↓
Evidence
   ↓
Candidate knowledge
   ↓
Critic
   ├── rejeita/corrige
   └── valida
          ↓
      Memory
```

Quando a fonte permitir, salvar retrieval hints para ensinar **como/onde procurar** posteriormente.

## 8. API/provider routing

Providers são capacidades disponíveis. O Brain não deve depender de uma lista fixa de modelos como verdade.

O catálogo estático pode bootstrapar o sistema; descoberta runtime determina disponibilidade atual. A política econômica atual pode restringir a seleção a capacidades gratuitas.

## 9. Fluxos principais

### Pergunta conhecida

`Chat → Brain → Memory → Chat`

### Pesquisa

`Chat → Brain → Memory → Discovery → Retrieval/Agent/API → Sandbox quando necessário → Evidence → Critic → Memory → Chat`

### Código

`Chat → Brain → Plan → Discovery → Code capability/Agent → Policy → Sandbox → Test/Build → Critic → Memory → Chat`

### Tarefa complexa

`Chat → Brain → Planner → ExecutionPlan → Function Splitter → Dispatcher → Actions/Workflow → Evidence → Critic → Memory → Chat`

## 10. O que não pertence ao núcleo

- LLM local baixado pelo app;
- Agent com LLM próprio;
- provider escolhido diretamente pelo usuário como requisito arquitetural;
- slash commands como mecanismo central;
- Room/schema do IaBrain;
- cópia de catálogos externos como fonte de verdade;
- execução de strings arbitrárias fora do Gateway/Policy/Sandbox.

## 11. Regra de consolidação

Antes de criar uma classe nova, localizar a responsabilidade equivalente no runtime atual. Se já existir, integrar/refatorar; não criar um segundo Registry, Router, Dispatcher, Agent Registry, Policy ou Memory apenas com outro nome.
