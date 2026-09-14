# BrainCode — Apresentação do Projeto

## O que é

BrainCode é uma arquitetura local-first em que **Brain pensa/roteia/autoriza, Agent executa capacidades e Sandbox executa sob limites**.

## Evolução recente

Os últimos 30 commits adicionaram:

- catálogo dinâmico de APIs;
- política free-only;
- waterfall/fallback;
- gateway Android;
- memória persistente de conhecimento;
- proveniência;
- Critic automático;
- reaproveitamento do engine local instalado.

## Conhecimento

```text
Problema
 ↓
Memória validada
 ↓ não encontrado
Router
 ↓
API gratuita
 ↓
Resposta + fonte
 ↓
Candidato
 ↓
Critic
 ↓
Conhecimento validado
```

O Brain não treina o LLM externo. Ele aprende conhecimento próprio e pode corrigi-lo.

## Proveniência

Quando possível, são guardados provider/model, URI e dados GitHub como repository/path/commit. A próxima etapa é transformar essas pistas em uma estratégia de recuperação automática.

## Android

A integração vertical comprovada é:

```text
UI → SandboxViewModel → BrainSandboxController
→ Bridge → CicloExecucaoPlano → PolicyBroker
→ CapabilityResolver → Sandbox
```

O caso comprovado é `sandbox.health`.

O `BrainApiGateway` já executa providers, mas a conversa Android ainda não deve ser descrita como integrada a ele até existir caller real comprovado.

## Segurança

Proot não é isolamento OS-level completo. Permanecem abertos network namespace, filesystem jail, process tree, resource enforcement e trust chain.

O Security Test Lab é sintético/determinístico e não equivale a ataque adversarial real.

## Estado

**Classificação:** protótipo avançado/runtime experimental com cliente Android offline e integração vertical parcial.

## Próximo marco

1. ligar chat ao gateway;
2. criar retrieval executor;
3. validar respostas de forma semântica;
4. versionar/deduplicar conhecimento;
5. repetir a matriz de testes no HEAD;
6. depois preparar evolução para servidor multiusuário.

## Documentação

- `docs/AUDITORIA_30_COMMITS_2026-09-14.md`
- `AUDITORIA_PESADA.md`
- `docs/MAPA_INTEGRACAO_2026-09-14.md`
- `PLANO_DE_ACAO.md`
- `ROADMAP_UNIFICADO.md`
- `TAREFAS_PENDENTES.md`
- `docs/SECURITY_TEST_LAB.md`
- `docs/SANDBOX_RELEASE_MIGRATION.md`