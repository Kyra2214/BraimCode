# Brain (nome provisório) — esqueleto do cérebro novo

Esqueleto inicial da estrutura que substitui o IaBrain como projeto
mantido. Nada aqui está implementado de verdade — são interfaces, data
classes e pontos de extensão (`TODO`) organizados segundo o plano de
ação e a ordem de fases A–H definida.

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

## Três decisões de arquitetura que já estão refletidas no código

1. **Contrato agnóstico de implementação** (`execution/SandboxContract.kt`):
   o Brain descreve `Job` (objetivo + requisitos de capacidade + contexto),
   nunca um comando de shell específico. O Sandbox decide como executar e
   devolve `JobResult` padronizado. Se o Sandbox mudar de proot para outra
   coisa amanhã, nada no Brain muda.

2. **Catálogo de APIs dinâmico, não números fixos** (`router/ApiCatalog.kt`):
   granularidade por `ProviderModel` (provider + modelo), com `LiveStats`
   observado em tempo real (quota restante estimada, último erro, latência,
   taxa de sucesso recente) — é isso que o `AIRouter` consulta para decidir,
   não a janela estática declarada pelo provedor.

3. **Memória desde a Fase D**, não como reforma tardia
   (`memory/ExperienceMemory.kt`): toda execução do fluxo de 8 partes gera
   uma `Experiencia`, sucesso ou falha. Pensado para SQLite/Room + JSON
   como primeira implementação; LEANN entra depois como busca semântica
   sobre esse mesmo histórico, sem substituir o registro básico.

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
