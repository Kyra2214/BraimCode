# Brain (nome provisório) — esqueleto do cérebro novo

Esqueleto inicial da estrutura que substitui o IaBrain como projeto
mantido. Os módulos são implementados seguindo o `ROADMAP_BRAIM_CONSOLIDADO.md`, um por vez, com validação e commit ao final de cada módulo.

## Módulo concluído: Fase A — contrato Brain ↔ Sandbox

`app/src/main/kotlin/com/brain/execution/SandboxContract.kt` define o contrato agnóstico de implementação para `Job`, `JobResult`, requisitos, contexto, orçamento, cancelamento, referências de secrets, manifesto de artefatos e eventos de execução. O `SandboxExecutor` continua congelado conforme o roadmap; nenhum executor real foi liberado.

Os contratos textuais correspondentes estão em `contracts/sandbox_job.md` e `contracts/sandbox_job_result.md`.

## Assets reaproveitados do IaBrain (dado real, não placeholder)

| Arquivo | Origem no IaBrain | Alimenta |
|---|---|---|
| `assets/ai_api_catalog.json` | `assets/ai_api_catalog.json` | `router/ApiCatalog` — 11 provedores de API gratuita já catalogados |
| `assets/explorer_china_seed.json` | `assets/explorer_china_seed.json` | `discovery/DiscoveryIntelligence` — seed do radar de descoberta |
| `assets/prompts_biblioteca.json` | `assets/prompts_biblioteca.json` | `prompt/PromptLibrary` — prompts reutilizáveis já existentes |
| `assets/comandos_catalogo.json` | `assets/comandos_catalogo.json` | Futuro Catálogo de Capacidades/Skills (Fase E) — 344 comandos com metadados (IA recomendada, modo de execução, nível) |
| `kotlin/.../discovery/DiscoveryIntelligence.kt` | `brain/ExplorerIntelligence.kt` | Pipeline de descoberta/validação/licença/ranking — reaproveitado quase integralmente, só o pacote mudou |

## Ordem de fases adotada

```
FASE A — Contrato Brain ↔ Sandbox (agnóstico de implementação)
FASE B — Estudo: Codex, OpenCode, Anthropic Skills, ECC, LangChain
FASE C — Cérebro mínimo: Secretário → Intent Parser → API Router → Roadmap → Prompt Generator
FASE D — Memória: experiências, histórico, resultados, erros, scores (SQLite/JSON; LEANN depois)
FASE E — Agentes: Skills, agentes especialistas, seleção por capacidade, fallback
FASE F — Workflows/Tarefas: ClawFlows, n8n
FASE G — APIs: catálogo, quotas dinâmicas, fallback, avaliação
FASE H — Integração: descongelar Sandbox, implementar o contrato, E2E completo
```

## Mapa: pacote → parte do fluxo original de 8 partes

| Pacote | Parte do fluxo | Fase |
|---|---|---|
| `core` | Parte 1 (secretário) e 2 (interpretar pedido) | C |
| `router` | Parte 3 e 5 (escolher API) | C (mínimo) / G (completo) |
| `prompt` | Parte 4 (gerar prompts) | C |
| `execution` | Parte 5/6 (contrato com o Sandbox) | A / H |
| `memory` | Aprendizado contínuo | D |
| `qa` | Parte 7 (ainda não criado neste esqueleto) | E |
| `delivery` | Parte 8 (ainda não criado neste esqueleto) | E/H |
| `orchestrator` | Visão geral, ainda não criado | H |

`qa`, `delivery` e `orchestrator` ficaram como pastas vazias por enquanto —
fazem mais sentido depois da Fase C (cérebro mínimo) estar de pé, porque
dependem do contrato do Sandbox (Fase A/H) para ter o que testar/entregar.

## Runtime executável do roadmap

A implementação funcional das fases C–H está em `brain_runtime/`, usando Python 3 e biblioteca padrão. Ela inclui `PolicyBroker`, `EventStore`, pipeline substituível, memória SQLite, registro de Skills, engine de workflows, catálogo dinâmico de APIs, executor Sandbox, QA gate e orquestração E2E. A suíte pode ser executada com:

```bash
python3 -m unittest discover -s tests
```

O estado detalhado de cada fase está em `docs/ROADMAP_IMPLEMENTADO.md`.
