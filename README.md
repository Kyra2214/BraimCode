# BrainCode — runtime Brain + Sandbox Mobile

BrainCode é um runtime experimental que combina **Policy, approval, eventos auditáveis, Sandbox, workflows, memória, routing, skills, QA e readiness** com um cliente Android offline baseado em RootFS/proot.

> **Estado real em 2026-09-13:** o projeto tem integração vertical real entre Android → Brain → Policy → Capability → Sandbox para `sandbox.health`, além de operações locais de Plugins, Workspace, Git status, Services/SQLite, TestLab, Security assessment e Toolchains. Porém **não está totalmente unificado**: existem APIs do `:brain` ainda fora do caminho Android, um workflow demonstrativo que não executa no Sandbox, EventStore Android em memória e um terminal livre que contorna o caminho Brain/Policy. Consulte `AUDITORIA_PESADA.md` antes de tratar qualquer componente como produção.

## Arquitetura

```text
BrainCode
├── brain_runtime/       # runtime Python de referência
├── brain/               # Brain Kotlin/JVM
├── android-module/      # bridge, policy/capabilities e Sandbox
├── app/                 # cliente Android Compose
├── contracts/           # contratos e invariantes
├── tests/               # testes Python
└── docs/                # auditorias, roadmap e documentação operacional
```

A fronteira conceitual é única:

```text
IaBrain / Policy pensa e autoriza
        ↓
Agent / Capability
        ↓
Sandbox executa sob limites
        ↓
Evidence / Events / Delivery
```

O Sandbox é o subsistema de execução do BrainCode; o app Android é atualmente o cliente offline que expõe apenas uma parte dessa arquitetura.

## Integração Android real

O caminho vertical comprovado no código é:

```text
SandboxViewModel.runBrainHealthCheck()
  → BrainSandboxController.healthCheck()
  → BrainSandboxExecutionBridge
  → CicloExecucaoPlano
  → PolicyBroker
  → AgentSandboxSession
  → CapabilityResolver
  → ManagedSandboxRuntime / proot
```

A UI também possui chamadas reais para `SandboxPlatform` em Plugins, Workspace, Git status, Services/SQLite, TestLab, Security assessment e Toolchains.

**Importante:** o botão de Workflow da aba Operações usa atualmente um workflow local/demonstrativo; ele não representa uma execução real no Sandbox. O terminal de comando livre também não passa pelo PolicyBroker/CapabilityResolver.

## Python runtime

A implementação de referência em `brain_runtime/` possui PolicyBroker, approval, pipeline, binding, sandbox, workflows, APIs, skills, memory/learning, observabilidade, Project Intelligence, readiness e release intelligence.

Executar a suíte documentada:

```bash
python3 -m unittest discover -s tests -q
```

A documentação histórica registra **134 testes Python aprovados**. Esta auditoria de 2026-09-13 não reexecutou a suíte nem o Gradle a partir do conector; os números históricos não devem ser tratados como uma nova execução desta rodada.

## Kotlin / Android

- `:brain`: Policy, Router, Planner, Skills, Workflows, Memory, APIs, Discovery, Events, Prompt e Execution.
- `:android-module`: `BrainSandboxController`, bridge, `PolicyBroker`, capabilities, sessões e runtime Sandbox.
- `:app`: Compose, lifecycle, RootFS, Plugins, Workspace, Git, Services, TestLab, Security, Toolchains e Operações.

A integração Brain ↔ app ainda é parcial. Componentes como `BrainExecutionCoordinator`, `DefaultPromptGenerator` e APIs avançadas existem e possuem testes, mas não são todos acionados pela UI.

## Segurança

O projeto possui hardening significativo, mas não deve ser tratado como container ou isolamento OS-level de produção. Proot, namespaces, cgroups/Bubblewrap/seccomp e garantias do kernel dependem da implantação.

O Security Test Lab atual possui duas camadas:

1. análise estática do workspace;
2. regressão sintética determinística e segura, sem payload adversarial real, rede ou alvo externo.

Isso é apropriado para regressão offline, mas **não equivale a um ataque adversarial real contra o Sandbox**.

## RootFS

Os RootFS homologados do antigo SandBox foram migrados sem rebuild:

- `0.3.3`
- `0.4.1`
- `0.5.0`

Tamanhos, SHA-256, sidecars e manifests foram preservados. Não reconstruir esses artefatos sem uma nova homologação explícita.

Documentação: `docs/SANDBOX_RELEASE_MIGRATION.md`.

## Documentação principal

- **Auditoria atual:** `AUDITORIA_PESADA.md`
- **Mapa de integração:** `docs/MAPA_INTEGRACAO_2026-09-13.md`
- **Apresentação do produto:** `docs/APRESENTACAO_BRAINCODE.md`
- **Plano de ação:** `PLANO_DE_ACAO.md`
- **Roadmap unificado:** `ROADMAP_UNIFICADO.md`
- **Tarefas e pendências reais:** `TAREFAS_PENDENTES.md`
- **Security Test Lab:** `docs/SECURITY_TEST_LAB.md`
- **Migração RootFS:** `docs/SANDBOX_RELEASE_MIGRATION.md`
- **Contratos:** `contracts/`

## Critério de conclusão

Uma funcionalidade só deve ser marcada como concluída quando houver:

1. implementação;
2. teste automatizado;
3. chamada real fora do próprio teste, a partir da UI, ViewModel ou runtime ensinado pelo README;
4. documentação coerente com o comportamento observado.

Esse critério existe justamente para impedir que código testado, porém órfão, seja apresentado como integração pronta.
