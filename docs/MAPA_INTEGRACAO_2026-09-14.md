# Mapa de Integração Real — BrainCode

**Data:** 2026-09-14.

## Caminho comprovado

```text
Android UI
 ↓
SandboxViewModel.runBrainHealthCheck()
 ↓
BrainSandboxController
 ↓
BrainSandboxExecutionBridge
 ↓
CicloExecucaoPlano
 ↓
PolicyBroker
 ↓
AgentSandboxSession
 ↓
CapabilityResolver
 ↓
ManagedSandboxRuntime / proot
```

Caso comprovado: `sandbox.health`.

## APIs / conhecimento

O `BrainApiGateway` existe no app e executa providers gratuitos via catálogo dinâmico, router, dispatcher e `ApiKeyStore`.

O ciclo de conhecimento é:

```text
problema
 ↓
KnowledgeMemory
 ↓
API gratuita
 ↓
resposta + fonte
 ↓
candidato
 ↓
Critic
 ↓
validado / pendente
```

**Ainda não comprovado:** caller real desse gateway a partir da conversa Android.

## Componentes parciais

- `BrainExecutionCoordinator`: existe/testado, sem caller Android comprovado.
- `DefaultPromptGenerator`: existe, sem ação de UI comprovada.
- APIs avançadas: catálogo/discovery/fallback implementados; transporte externo no fluxo de conversa ainda não comprovado pela UI.
- `FileEventStore`: implementação existe; o caminho principal da fachada precisa ser auditado para persistência efetiva.
- Security Regression Corpus: implementação persistente existe; o fluxo principal da UI ainda precisa fechar essa integração.
- Workflow da fachada: demonstrativo, não Sandbox real.

## Regra

Nunca marcar como integrado somente por classe, teste ou instanciação indireta. A prova mínima é:

```text
implementação + teste + caller real + evidência + documentação
```

## Próximas conexões

1. chat Android → `BrainApiGateway`;
2. retrieval executor → GitHub/search;
3. validação semântica → Sandbox/segunda fonte;
4. EventStore/Security pelo caminho principal;
5. testes de wiring/orphan.