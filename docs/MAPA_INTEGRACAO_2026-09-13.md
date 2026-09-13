# Mapa de Integração Real — BrainCode

**Data:** 2026-09-13  
**Base:** commit `b9576e43023d4e613d07c481d90c953bb8b51b7a` + auditoria documental/código desta rodada.

## 1. Caminho realmente integrado

```text
Android UI
  ↓
SandboxViewModel
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
ManagedSandboxRuntime
  ↓
proot / RootFS
  ↓
ResultadoCiclo / evidência
```

O caso de uso real atualmente comprovado é `sandbox.health`.

## 2. Operações locais conectadas à UI

```text
Operações
├── TestLab
├── Security assessment
├── Approval demo / resume
├── Workspace
├── Git status
├── SQLite Service
├── Brain Skills / Memory / Discovery
├── Workflow local demonstrativo
├── ObservableDelivery local
└── Toolchains

Plugins / Ferramentas
└── catálogo + install/remove/rollback local
```

Essas chamadas partem do `SandboxViewModel` e usam `SandboxPlatform` ou `BrainIntegrationFacade`.

## 3. Componentes que existem, mas não são caminho completo do usuário

### `BrainExecutionCoordinator`

Existe no `:brain` e possui testes. Não é instanciado pelo fluxo Android atual.

### `DefaultPromptGenerator`

Existe no `:brain` e foi exposto programaticamente pela `BrainIntegrationFacade`, mas não existe uma ação correspondente na UI.

### APIs avançadas do `:brain`

Catálogos, discovery e fallback existem, porém não equivalem a chamadas reais de providers externos no Android offline.

### `FileEventStore`

Existe e é testado. O `BrainIntegrationFacade` usa `InMemoryEventStore`, então os eventos Android dessa fachada não sobrevivem ao reinício.

### `SecurityRegressionCorpus`

Existe e é usado por `SandboxPlatform.runSecurityRegression()`, mas a ação atual da UI chama diretamente `security.evaluate(..., emptyList())`. Portanto a persistência do corpus não fecha pelo caminho principal da tela de Security.

## 4. Caminhos que não devem ser confundidos com execução protegida

### Terminal livre

`SandboxViewModel.runCommand()` recebe texto da UI e executa `/bin/bash -c` no `ManagedSandboxRuntime`.

Esse caminho é útil para diagnóstico/manual testing, mas **não passa pelo PolicyBroker + CapabilityResolver**. Deve ser tratado como modo de manutenção/diagnóstico, não como prova da arquitetura deny-by-default.

### Workflow local

`BrainIntegrationFacade.runHealthWorkflow()` usa `WorkflowEngine` e uma lambda local que retorna sucesso. Não chama Sandbox. É uma demonstração do motor de workflow.

### Security regression

Os probes baseline são sintéticos e determinísticos. Não executam payloads adversariais reais.

## 5. Fronteira Python × Kotlin

O Python (`brain_runtime/`) é o runtime de referência. O Kotlin (`brain/`) é a implementação JVM/Android. Eles não precisam compartilhar objetos diretamente.

O problema a evitar é outro: marcar uma capacidade Kotlin como integrada ao Android apenas porque existe e passa em teste próprio.

## 6. Estado de integração

| Camada | Estado |
|---|---|
| Python runtime | funcional/testado historicamente |
| Android Sandbox | integrado em operações locais |
| Brain → Sandbox | integrado para `sandbox.health` e demo de approval/resume |
| Brain avançado → Android | parcial |
| Security regression persistente → UI | parcial |
| Events persistentes → Android facade | pendente |
| Prompt generator → UI | pendente |
| Coordinator avançado → UI | pendente |
| Terminal livre protegido por Policy | pendente |
| Device/emulador ARM64 | validação externa ainda necessária |

## 7. Regra para futuras integrações

Nunca marcar uma peça como integrada somente por:

- existência da classe;
- teste unitário;
- instanciação dentro de outro componente que a UI não chama;
- método público sem caller real.

A prova mínima é:

```text
Implementação
+ teste
+ caller real
+ evidência observável
+ documentação coerente
```
