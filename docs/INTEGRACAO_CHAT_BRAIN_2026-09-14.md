# Integração do Chat Android com o Brain — 2026-09-14

## Estado

O Chat é a superfície de conversa do aplicativo. Não existe mais download nem execução de LLM local embutida no aplicativo.

## Caminho atual

```text
MainActivity
  ↓
SandboxViewModel.sendChatMessage()
  ↓
BrainApiGateway.complete(prompt)
  ↓
Memória validada
  ├─ encontrou → resposta
  └─ não encontrou
       ↓
   Router / DynamicFreeApiCatalog
       ↓
   ProviderDispatcher
       ↓
   API gratuita selecionada pelo Brain
       ↓
   KnowledgeLearningCycle + Critic
       ↓
   resposta para SandboxViewModel
       ↓
   UI: Chat
```

## Regra arquitetural fechada

> O usuário conversa somente com o Brain através do Chat.

A UI não escolhe provider, modelo, endpoint ou mecanismo de execução. Essas decisões pertencem ao Brain.

Não existe mais caminho de:

```text
Chat → LLM local → llama.cpp → GGUF
```

Nem download de modelo local durante a preparação ou validação do aplicativo.

## Agents

A execução especializada será feita por Agents amarrados, sem LLM próprio por padrão.

```text
Brain
  ↓
Agent especializado
  ↓
Capability permitida
  ↓
Sandbox
  ↓
Evidence / resultado
  ↓
Brain / Critic
```

O Agent recebe uma missão e ferramentas/capacidades permitidas. Ele não cria um objetivo próprio nem conversa diretamente com o usuário.

## Sandbox Research

Para pesquisa, o Agent poderá usar a capacidade de Terminal Research no Sandbox. O terminal pesquisa fontes externas e devolve evidências e provenance ao Brain. O Agent não precisa carregar um motor de linguagem próprio.

## Critic e Memory

Respostas externas continuam passando pelo `KnowledgeLearningCycle` e pelo Critic antes de serem tratadas como conhecimento reutilizável.

O objetivo é manter:

```text
resposta externa
  ↓
evidência / fonte
  ↓
Critic
  ↓
Memory
```

## Observação

O Chat não é uma identidade de modelo. É apenas o canal de conversa do usuário com o Brain.
