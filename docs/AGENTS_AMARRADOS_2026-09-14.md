# Agents Amarrados — BrainCode

**Data:** 2026-09-14  
**Status:** arquitetura adotada; implementação inicial criada

## Decisão

O BrainCode não terá um LLM próprio dentro de cada Agent.

Um Agent é um executor especializado e amarrado a uma lista explícita de capabilities. Ele recebe uma missão criada pelo Brain, executa somente as capacidades autorizadas e devolve evidências/resultado.

## Separação de responsabilidades

```text
Usuário
  ↓
Chat
  ↓
Brain
  ├── Memory
  ├── decisão/orquestração
  ├── escolha do Agent
  └── seleção de API quando necessário
          ↓
       Agent amarrado
          ↓
      Capability
          ↓
        Sandbox
          ↓
 Evidence / resultado
          ↓
       Critic / Memory
          ↓
       Brain / Chat
```

### Brain

Decide o objetivo, decompõe a tarefa, escolhe o Agent e controla o fluxo.

### Agent

Executa uma missão específica. Não inventa objetivo, não conversa com o usuário e não possui LLM próprio por padrão.

### Capability

É a ferramenta permitida ao Agent. Exemplos: `TERMINAL_RESEARCH`, `GITHUB`, `CODE_BUILD`, `CODE_TEST`, `FILE_READ`.

### Sandbox

É o ambiente de execução protegido. O Agent não recebe shell arbitrário; ele solicita capabilities autorizadas.

### Evidence

O resultado deve carregar evidência e, quando aplicável, fonte/provenance.

### Critic

Avalia o resultado antes que ele seja promovido a conhecimento reutilizável.

## Agents iniciais

### ResearchAgent

Capabilities:

- `TERMINAL_RESEARCH`
- `WEB_SEARCH`
- `GITHUB`

Uso: pesquisa de documentação, GitHub e fontes externas pelo modo Terminal Research do Sandbox.

### CodeAgent

Capabilities:

- `FILE_READ`
- `FILE_WRITE`
- `WORKSPACE`
- `CODE_BUILD`
- `CODE_TEST`
- `GIT`

Uso: tarefas de código, build, testes e alterações controladas no workspace.

## Regra importante

O Agent não recebe o prompt completo como se fosse um chatbot e não escolhe livremente comandos.

O Brain entrega uma missão estruturada:

```text
AgentMission
├── id
├── objective
├── requiredCapabilities
└── parameters
```

O Agent só executa se todas as capabilities exigidas estiverem no seu allowlist.

## Quando um LLM pode aparecer

Um LLM pode ser usado pelo Brain ou por uma API especializada quando a tarefa realmente exigir raciocínio/geração. Isso é uma capability externa do fluxo, não um requisito estrutural dos Agents.

Portanto:

```text
Agent ≠ LLM
Agent = executor especializado
```

## Resultado desejado

O BrainCode fica menor, mais previsível, auditável e barato. Não existe um motor de agentes separado para manter vários LLMs executando em paralelo.
