# Roadmap Unificado — BrainCode

**Atualização:** 2026-09-14.

## Critério

✅ = implementado + testado + caller real + evidência.  
🟡 = implementado/parcial, mas integração ou evidência de produto incompleta.  
❌ = design/backlog.

## Fase 0 — Sandbox / RootFS

**Status: ✅ fundação homologada.**

RootFS `0.3.3`, `0.4.1` e `0.5.0` migrados sem rebuild, com tamanhos/SHA-256 preservados. Não reconstruir sem nova homologação.

## Fase 1 — Brain base

**Status: 🟡 integração Android parcial.**

Policy, planner, routing, memória, skills, workflows, APIs e execution existem no runtime. O caminho Android comprovado é `sandbox.health` via Bridge → Policy → Capability → Sandbox.

## Fase 2 — APIs dinâmicas e conhecimento

**Status: 🟡 implementação forte; integração de conversa Android ainda não comprovada.**

Concluído:

- descoberta dinâmica de modelos;
- catálogo free-only;
- refresh runtime;
- waterfall/fallback;
- gateway Android;
- memória persistente de conhecimento;
- proveniência;
- Critic automático.

Pendente:

- retrieval automático;
- Critic semântico;
- deduplicação/versionamento;
- escopos e índice escalável.

## Fase 3 — Integração Android

**Status: 🟡.**

Comprovado:

```text
UI → SandboxViewModel → BrainSandboxController
→ Bridge → CicloExecucaoPlano → PolicyBroker
→ CapabilityResolver → Sandbox
```

A conversa Android ainda precisa comprovar caller de `BrainApiGateway`.

## Fase 4 — Hardening

**Status: 🟡.**

Autorização, superfície de capability e rollback receberam correções reais. Permanecem pendentes isolamento OS-level, trust chain, credentials, SSRF, leases e testes arquiteturais.

## Fase 5 — Security Test Lab

**Status: 🟡.**

Há análise e regressão sintética/determinística. Não equivale a ataque adversarial real nem prova isolamento OS-level.

## Fase 6 — Escalabilidade futura

**Status: ❌ backlog.**

A abstração `KnowledgeMemory` deve permitir evolução de armazenamento local para SQLite/FTS e, futuramente, banco/indexador de servidor. Separar conhecimento global validado de memória privada de usuário/projeto.

## Próximo marco

O próximo marco é **fechar conexões**, não criar mais classes:

1. ligar chat Android ao gateway;
2. criar retrieval executor;
3. validar respostas por Sandbox/segunda fonte;
4. adicionar deduplicação/versionamento;
5. repetir toda a matriz de testes no HEAD;
6. só então avançar para servidor multiusuário.
