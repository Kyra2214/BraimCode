# BrainCode — Apresentação do Projeto

## O que é

O BrainCode é uma arquitetura local-first que une **inteligência/orquestração, autorização, execução isolada e evidência**.

A ideia central é simples:

> **Brain pensa e autoriza. Agent trabalha. Sandbox executa. Evidence registra.**

No produto Android atual, o Sandbox é o subsistema de execução do BrainCode e o app funciona offline, usando RootFS/proot e estado local.

## Arquitetura

```text
                    BRAINCODE
                        │
             ┌──────────┴──────────┐
             │                     │
          Brain                 Sandbox
             │                     │
      Policy / Planner       Capabilities
      Router / Skills             │
      Workflows / Memory          │
             │               Managed Runtime
             └──────────┬──────────┘
                        │
                   Evidence
                   Events / QA
                        │
                    Delivery
```

## Python

`brain_runtime/` é o runtime de referência. Ele concentra a implementação mais completa de:

- PolicyBroker e approval;
- pipeline e planner;
- binding e autorização;
- Sandbox;
- workflows persistentes;
- APIs/providers;
- skills e provenance;
- memory/learning;
- observabilidade;
- Project Intelligence;
- readiness e release intelligence.

## Android

O Android é a superfície de produto offline.

Hoje ele possui caminhos reais para:

- baixar/preparar RootFS;
- lifecycle e reset do Sandbox;
- execução proot;
- plugins e ferramentas;
- Workspace;
- Git status;
- Services/SQLite;
- TestLab;
- Security assessment;
- Toolchains;
- Memory/Skills/Discovery locais;
- entrega local observável;
- Brain → Policy → Sandbox para `sandbox.health`.

## A integração Brain + Sandbox

O primeiro caminho vertical real é:

```text
UI
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
```

Isso é uma integração real, não apenas um teste unitário.

## O que ainda não é produto completo

O projeto ainda não deve ser apresentado como plataforma totalmente unificada ou como ambiente de produção isolado.

Existem componentes avançados que ainda são biblioteca/API:

- `BrainExecutionCoordinator` não está no caminho Android principal;
- `DefaultPromptGenerator` não possui ação de UI;
- APIs avançadas do `:brain` ainda não são providers externos reais no Android offline;
- EventStore da fachada Android ainda é em memória;
- Security Regression Corpus possui implementação persistente, mas o botão atual de Security não fecha esse caminho;
- workflow da fachada é demonstrativo e não executa no Sandbox;
- terminal livre da UI não passa pelo PolicyBroker/CapabilityResolver.

## Segurança

O projeto possui hardening significativo, mas proot não equivale sozinho a isolamento de produção.

O modo de segurança atual distingue:

- avaliação estática;
- regressão sintética determinística;
- execução real no Sandbox;
- controles OS-level dependentes do host.

Não há simulação de segurança apresentada como prova de isolamento real.

## RootFS

Os RootFS homologados do antigo SandBox foram migrados para o BrainCode sem rebuild:

- 0.3.3
- 0.4.1
- 0.5.0

Os bytes, tamanhos, SHA-256 e sidecars foram preservados.

## Estado do projeto

**Classificação atual:** protótipo avançado / runtime experimental com cliente Android offline funcional e integração vertical parcial Brain ↔ Sandbox.

**Não classificar como:**

- plataforma de produção plenamente isolada;
- Brain totalmente unificado com todos os módulos Android;
- ataque adversarial real validado;
- providers externos funcionando sem credenciais/rede;
- infraestrutura distribuída pronta.

## Próximo marco técnico

O próximo marco não é criar mais classes. É **fechar as conexões existentes**:

1. retirar ou proteger o terminal livre;
2. ligar Security Regression Corpus ao fluxo UI;
3. persistir EventStore Android;
4. decidir e ligar `BrainExecutionCoordinator` quando houver caso de uso real;
5. decidir e ligar `DefaultPromptGenerator` à UI;
6. separar definitivamente caminhos demonstrativos de execução real;
7. repetir build/testes;
8. validar em ARM64 device/emulador.

## Documentação relacionada

- `AUDITORIA_PESADA.md` — auditoria técnica completa;
- `docs/MAPA_INTEGRACAO_2026-09-13.md` — mapa de chamadas reais e órfãos;
- `PLANO_DE_ACAO.md` — plano técnico;
- `ROADMAP_UNIFICADO.md` — evolução por fases;
- `TAREFAS_PENDENTES.md` — backlog verdadeiro;
- `docs/SECURITY_TEST_LAB.md` — laboratório de segurança;
- `docs/SANDBOX_RELEASE_MIGRATION.md` — migração dos RootFS.
