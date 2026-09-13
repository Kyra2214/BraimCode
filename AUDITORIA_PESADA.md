# Auditoria Pesada — BrainCode

**Data:** 2026-09-13  
**Commit auditado:** `b9576e43023d4e613d07c481d90c953bb8b51b7a`  
**Escopo:** `brain_runtime/`, `brain/`, `android-module/`, `app/`, testes, contratos, RootFS/migração e documentação.  
**Método:** inspeção do código publicado, rastreamento de instanciação/chamadas fora do próprio arquivo/teste, comparação entre README/roadmap/plano/tarefas e código real.

## Veredito executivo

O BrainCode está **funcionalmente integrado em parte**, mas **não está totalmente unificado**. A documentação anterior inflava o estado de integração e esta auditoria corrige isso.

Hoje existem três superfícies reais:

1. **`brain_runtime/` Python:** runtime de referência, com Policy, approval, pipeline, sandbox, workflows, APIs, skills, memória, observabilidade, Project Intelligence e readiness. A suíte documentada possui 134 testes Python aprovados, mas esta auditoria não reexecutou a suíte no ambiente do GitHub connector.
2. **Android `:app` + `:android-module`:** caminho real acionado pela UI. RootFS, proot, lifecycle, plugins, Workspace, Git status, Services/SQLite, TestLab, Security assessment, Toolchains e a primeira ponte Brain → Sandbox (`sandbox.health`) têm chamadas reais a partir do app.
3. **`:brain` Kotlin:** biblioteca funcional e testada, mas ainda com várias ilhas que não são o caminho de execução do usuário. `BrainExecutionCoordinator`, geração de prompts, APIs avançadas e parte do EventStore permanecem fora do fluxo principal Android.

**Conclusão:** não há base para dizer que "tudo está conectado". Há uma integração vertical real, mas existem caminhos simulados, APIs órfãs e divergências documentais que precisam continuar explícitas.

## Matriz de integração real

| Componente | Código existe | Teste próprio | Chamado pelo app | Classificação |
|---|---:|---:|---:|---|
| RootFS/proot/lifecycle | Sim | Sim | Sim | **Integrado** |
| Plugin catalog/install/remove/rollback local | Sim | Sim | Sim | **Integrado localmente** |
| Workspace | Sim | Sim | Sim | **Integrado básico** |
| Git status | Sim | Sim | Sim | **Integrado básico** |
| SQLite Service | Sim | Sim | Sim | **Integrado básico** |
| TestLab | Sim | Sim | Sim | **Integrado básico** |
| SecurityAssessmentEngine | Sim | Sim | Sim | **Integrado como avaliação** |
| SecurityRegressionCorpus | Sim | Sim | Indiretamente apenas; o botão atual chama `security.evaluate(..., emptyList())` | **Parcial / ligação incompleta** |
| ToolchainManager | Sim | Sim | Sim | **Integrado básico** |
| BrainSandboxController / `sandbox.health` | Sim | Sim | Sim | **Integração vertical real** |
| Planos + aprovação/retomada | Sim | Sim | Sim, via demo de `sandbox.health` | **Integrado em escopo estreito** |
| `BrainIntegrationFacade` Skills/Memory/Discovery | Sim | Sim | Sim | **Integrado localmente, fora da Policy/Sandbox** |
| `BrainIntegrationFacade.runHealthWorkflow()` | Sim | Sim | Sim | **Caminho demonstrativo, não execução Sandbox real** |
| `BrainExecutionCoordinator` | Sim | Sim | Não | **Órfão do caminho Android** |
| `DefaultPromptGenerator` | Sim | Sim | Não | **Órfão da UI** |
| APIs avançadas do `:brain` | Sim | Sim | Não | **Órfãs/parciais** |
| `FileEventStore` | Sim | Sim | Não no facade Android atual | **Disponível, mas não usado pelo fluxo Android** |
| `InMemoryEventStore` no `BrainIntegrationFacade` | Sim | Sim | Sim | **Persistência falsa para eventos locais** |
| `HttpProviderClient` / providers externos | Sim | Sim | Não | **Fora do produto offline atual** |

## Achados críticos

### C1 — Comando livre da UI ainda contorna a arquitetura Brain → Policy → Capability

`SandboxViewModel.runCommand()` envia `listOf("/bin/bash", "-c", command)` diretamente ao `ManagedSandboxRuntime`. A tela `CommandSection` também aceita texto livre.

Isso é deliberadamente útil para validação manual, mas **não pode ser descrito como caminho protegido pelo PolicyBroker**. O caminho protegido é o `BrainSandboxController`/`CapabilityResolver`, não o terminal livre.

**Impacto:** o usuário consegue executar comandos fora do catálogo de capabilities. Isso mantém uma superfície de execução privilegiada paralela à arquitetura de autorização.

**Classificação:** CRÍTICO para o modelo de segurança; não significa que o proot seja inútil, mas significa que a UI não deve ser tratada como execução deny-by-default.

### C2 — `BrainIntegrationFacade.runHealthWorkflow()` é um workflow demonstrativo

A fachada cria um `WorkflowEngine`, mas o executor do nó retorna `WorkflowStepResult(success = true)` após uma lambda local. Ele não chama `BrainSandboxController`, `PolicyBroker`, `CapabilityResolver` ou `ManagedSandboxRuntime`.

Portanto o botão **Workflow** prova o motor de workflow local, mas **não prova execução Brain → Policy → Sandbox**.

**Classificação:** ALTO — documentação deve chamá-lo de workflow local/demonstrativo até que exista uma execução autorizada real.

### C3 — Security Regression Corpus existe, mas o botão atual não usa a suíte persistente

`SandboxPlatform.runSecurityRegression()` conecta scanner → probes determinísticos → `SecurityAssessmentEngine` → corpus persistente. Porém `SandboxViewModel.runSecurityAssessment()` chama diretamente `securityScanner.scan()` e `security.evaluate(..., emptyList())`.

Como `SecurityTestLab` trata lista vazia como resultados sintéticos determinísticos, o botão atual executa uma avaliação sintética, mas **não percorre o método `runSecurityRegression()` que registra o corpus**.

**Classificação:** ALTO — a capacidade está implementada, porém o caminho UI não fecha a integração planejada.

### C4 — EventStore do Android está em memória

`BrainIntegrationFacade` instancia `InMemoryEventStore`, apesar de o `:brain` possuir `FileEventStore` testado. Assim, `localEvents()` e `localEventsHealthy()` funcionam durante a vida da instância, mas não constituem persistência local após reinício do app.

**Classificação:** ALTO para auditoria/observabilidade; a implementação persistente já existe, portanto o problema é de wiring.

## Achados altos

### H1 — `BrainExecutionCoordinator` não é instanciado pelo caminho Android

Existe uma implementação avançada no `:brain`, com Policy, approval, retry, eventos e memória, mas o app usa `BrainSandboxController` para a fatia atual. O coordenador permanece uma API de biblioteca/teste, não uma etapa do fluxo Operações.

### H2 — `DefaultPromptGenerator` foi exposto na fachada, mas não existe ação correspondente na UI

A fachada possui `generatePrompts()` e `generateCorrectionPrompt()`, porém a UI atual não oferece uma operação que as invoque. É API acessível por código, não feature entregue ao usuário.

### H3 — Toolchain rollback não é rollback transacional completo do estado do sistema

O snapshot registra os pacotes do perfil e o estado anterior, mas o rollback remove os pacotes declarados. Ele não restaura versões anteriores de pacotes nem um snapshot completo do sistema de pacotes. Portanto o nome "rollback transacional" é forte demais.

### H4 — Security Test Lab atual é regressão sintética, não ataque adversarial real

Os cenários baseline geram `SecurityProbeResult` sintéticos. Isso é correto para uma suíte offline determinística e segura, mas não deve ser apresentado como execução de payloads de ataque reais dentro do Sandbox.

### H5 — Discovery Android é catálogo built-in demonstrativo

`BrainIntegrationFacade.discoverBuiltInCandidate()` monta uma única fonte/candidato BrainCode em memória. Isso valida a pipeline de discovery, não um mecanismo de descoberta externa.

## Achados médios

- `InMemoryExperienceMemory` permanece útil para testes/fallback, mas o app usa `FileExperienceMemory`.
- `RemotePluginCatalog` não é transporte remoto: no caminho offline ele importa snapshots explícitos. Isso está correto, mas deve continuar nomeado como catálogo remoto por snapshot, não como sincronização remota.
- `HttpProviderClient` e providers externos continuam fora do escopo Android offline.
- O RootFS migrado do SandBox deve continuar imutável; não há indicação nesta auditoria de que seja necessário reconstruí-lo.
- `reference/braincode-python/` é histórico e não participa do build ativo.

## O que está realmente conectado

O caminho vertical mais forte atualmente é:

```text
UI
 ↓
SandboxViewModel.runBrainHealthCheck()
 ↓
BrainSandboxController.healthCheck()
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
 ↓
ResultadoCiclo / evidência
 ↓
UI
```

O `SandboxPlatform` também possui caminhos reais para:

```text
UI → SandboxViewModel → SandboxPlatform
   ├─ Plugins
   ├─ Workspace
   ├─ Git status
   ├─ Services / SQLite
   ├─ TestLab
   ├─ Security assessment
   └─ Toolchains
```

Já o caminho abaixo **não é real ainda**:

```text
UI → BrainExecutionCoordinator → Policy → Sandbox → resultado
```

E o caminho abaixo é apenas demonstrativo:

```text
UI → BrainIntegrationFacade.runHealthWorkflow()
   → WorkflowEngine → lambda local → sucesso
```

## Verificação de órfãos

Foram considerados órfãos os componentes que possuem implementação/teste mas não possuem chamada fora do próprio módulo/teste. Os principais encontrados nesta rodada são:

- `BrainExecutionCoordinator` no app Android;
- `DefaultPromptGenerator` na UI;
- APIs avançadas do `:brain`;
- `FileEventStore` no facade Android;
- `SecurityRegressionCorpus` no botão de Security;
- parte do fluxo de prompts/correção;
- transporte remoto real de plugins, que é propositalmente fora do offline.

## Estado de segurança

O projeto possui hardening relevante, mas o Android não deve ser chamado de ambiente isolado de produção. O terminal livre, proot e os controles de recurso continuam sujeitos às garantias do host/kernel. O runtime Python também depende de isolamento OS-level fornecido pelo host para garantias fortes.

## Validação desta auditoria

Esta rodada foi uma **auditoria estática e de integração no código publicado**. O commit auditado é `b9576e43023d4e613d07c481d90c953bb8b51b7a`. Não houve execução local de Gradle/Python a partir deste conector e não existe evidência de workflow CI para este commit nesta rodada. Portanto, esta auditoria não inventa um novo "PASS" de build/teste.

As validações funcionais anteriores continuam sendo histórico do projeto, mas precisam ser repetidas após qualquer correção de código.

## Próximas correções prioritárias

1. Fechar ou restringir o comando livre da UI; se mantido para diagnóstico, marcar explicitamente como modo de manutenção e impedir classificação como caminho autorizado.
2. Fazer o botão Security usar `runSecurityRegression()` e registrar o corpus.
3. Trocar `InMemoryEventStore` por `FileEventStore` no facade Android.
4. Separar claramente `WorkflowEngine` demonstrativo de execução real em Sandbox.
5. Integrar `BrainExecutionCoordinator` somente quando houver um caso de uso UI real que precise de múltiplas etapas; até lá, mantê-lo como API interna documentada.
6. Expor geração de prompts na UI ou remover a promessa de feature disponível.
7. Reclassificar Toolchain rollback como rollback de pacote declarado, ou implementar restauração de versão real.
8. Reexecutar testes/build e validar em ARM64 device/emulador após as correções.

## Referências

- `README.md`
- `PLANO_DE_ACAO.md`
- `ROADMAP_UNIFICADO.md`
- `TAREFAS_PENDENTES.md`
- `docs/BACKLOG_OFFLINE_ITEM_5.md`
- `docs/SECURITY_TEST_LAB.md`
- `docs/SANDBOX_RELEASE_MIGRATION.md`
