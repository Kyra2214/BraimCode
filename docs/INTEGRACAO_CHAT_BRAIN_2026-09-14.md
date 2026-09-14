# Integração do chat Android com o Brain — 2026-09-14

## Estado

A integração que antes estava apenas pendente de comprovação foi conectada no caller real do chat.

## Caminho comprovado no código

```text
MainActivity
  ↓
SandboxViewModel.sendChatMessage()
  ↓
BrainApiGateway.complete(prompt)
  ↓
memória validada / Router / DynamicFreeApiCatalog
  ↓
ProviderDispatcher
  ↓
API gratuita selecionada pelo Brain
  ↓
KnowledgeLearningCycle + Critic
  ↓
resposta para SandboxViewModel
  ↓
UI
```

`MainActivity` continua chamando `viewModel.sendChatMessage()`. O ponto importante é que `sendChatMessage()` deixou de executar `LocalModelManifestLoader`, `ensureLocalModelLinkedIntoRootfs()` e `ManagedSandboxRuntime.execute()` para responder ao chat. Ele entrega a mensagem ao `BrainApiGateway`.

## Regra arquitetural fechada

> O usuário conversa somente com o Brain.

A UI não escolhe provider, modelo, endpoint, fallback ou mecanismo de inferência. Essas decisões pertencem ao Brain.

## O que isso não significa ainda

Esta mudança não transforma o Critic em validador semântico completo. O Critic atual continua sendo o gate estrutural/evidencial documentado na auditoria.

Também não significa que toda mensagem necessariamente usará uma API externa: `BrainApiGateway.complete()` tenta primeiro o conhecimento validado em memória. Se não houver conhecimento válido, o Brain roteia para as APIs gratuitas disponíveis.

## Observação sobre o Sandbox

O Sandbox continua existindo como executor protegido do Brain para tarefas operacionais. Ele não é o canal de conversa do usuário.

## Validação pendente

O commit desta conexão ainda precisa passar pela matriz de build/testes do projeto antes de ser marcado como funcionalmente validado em CI. A ausência de workflow associado ao commit não deve ser interpretada como PASS ou FAIL.