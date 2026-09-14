# Auditoria Pesada — BrainCode

**Atualização:** 2026-09-14  
**Auditoria detalhada dos últimos 30 commits:** `docs/AUDITORIA_30_COMMITS_2026-09-14.md`.

## Regra

> Implementação + teste + caller real + evidência observável + documentação coerente.

Código ou teste isolado não prova integração de produto.

## Estado atual

A janela recente adicionou:

- descoberta dinâmica de modelos por provider;
- catálogo automático **free-only**;
- refresh de catálogo antes do routing;
- substituição de modelos que desapareceram;
- waterfall/fallback entre APIs gratuitas;
- gateway Android para execução de providers;
- memória persistente de conhecimento;
- proveniência com URI/repository/path/commit quando identificáveis;
- aprendizado com candidato → Critic → conhecimento validado;
- reaproveitamento do engine local já instalado no RootFS.

## Segurança ainda aberta

### P0

- isolamento de rede OS-level;
- jail de filesystem OS-level;
- isolamento da árvore de processos;
- enforcement OS-level de CPU/memória/PIDs/FDs/disco;
- trust chain autenticada dos RootFS/manifests.

### P1

- teste arquitetural Planner → ExecutionPlan → Policy → AuthorizedPlan → Agent → Sandbox;
- bind completo de credentials;
- SSRF/DNS rebinding;
- rotação de EventStore preservando hash-chain;
- fencing de workflow leases;
- testes de wiring/orphan;
- validação semântica do conhecimento;
- retrieval automático baseado em proveniência;
- deduplicação/versionamento/escopos de memória.

## Integração Android

Comprovado:

```text
SandboxViewModel
 → BrainSandboxController
 → BrainSandboxExecutionBridge
 → CicloExecucaoPlano
 → PolicyBroker
 → AgentSandboxSession
 → CapabilityResolver
 → ManagedSandboxRuntime / proot
```

O caso comprovado é `sandbox.health`.

`BrainApiGateway` está implementado e executa providers, mas **não marcar o chat Android como integrado** até comprovar caller real da tela de conversa.

`BrainExecutionCoordinator`, `DefaultPromptGenerator` e APIs avançadas continuam parciais quanto ao caminho Android.

## Memória / Critic

Respostas externas são candidatas. O recall automático usa somente conhecimento validado.

O Critic atual é estrutural/evidencial:

- vazio → REJECT;
- sem fonte → UNCERTAIN;
- resposta + fonte → ACCEPT moderado.

Não é validação semântica completa.

## Validação

A matriz de PASS de 2026-09-13 não deve ser transferida automaticamente para o HEAD de 2026-09-14, porque houve código novo depois dela.

Executar novamente antes de declarar PASS:

```bash
./gradlew test
./gradlew check
./gradlew :app:assembleDebug
python3 -m unittest discover -s tests -p 'test_*.py' -v
bash scripts/validate-release-readiness.sh
bash -n scripts/*.sh rootfs-builder/*.sh
```

## Resultado

O projeto avançou de catálogo estático para **descoberta dinâmica + free-only + fallback + execução + memória + proveniência + Critic**. A próxima fronteira é recuperar soluções diretamente pela memória/proveniência e fechar a integração do chat sem mascarar componentes ainda não comprovados.