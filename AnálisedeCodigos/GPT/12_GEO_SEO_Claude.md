# 12 — zubair-trabzada/geo-seo-claude

## Objetivo

Estudar um exemplo completo de Skill especializada com sub-Skills, subagentes, comandos, scoring, relatórios, atualização e controle de qualidade.

## Arquitetura encontrada

O projeto possui uma Skill principal `geo/SKILL.md`, várias sub-Skills e cinco subagentes especializados. O fluxo de auditoria é dividido em descoberta, análise paralela e síntese.

## Esse padrão é quase um modelo do Braim

### Fase 1 — Discovery

A Skill identifica tipo de negócio, coleta páginas e prepara contexto.

### Fase 2 — Parallel Analysis

Cinco subagentes analisam áreas diferentes.

### Fase 3 — Synthesis

O resultado é agregado, pontuado e transformado em plano de ação.

O Braim precisa exatamente desse conceito para projetos complexos.

## Subagentes

O projeto separa:

- AI visibility;
- platform analysis;
- technical;
- content;
- schema.

Isso mostra que especialista deve ter **escopo estreito**, não prompt gigante.

## Scoring

A auditoria calcula score composto com pesos por categoria. O Braim deve aplicar scoring a agentes/APIs/Skills:

```text
score = quality + reliability + latency + cost + historical_success
```

## Comandos

O conjunto `/geo audit`, `/geo quick`, `/geo technical`, `/geo content`, `/geo schema`, `/geo report` etc. mostra como uma Skill pode oferecer uma superfície simples para vários procedimentos.

No Braim, esses comandos seriam capabilities registradas, não hardcoded no parser.

## Quality gates

O projeto define limites de crawl, timeout, rate limiting, robots.txt e deduplicação. Isso é valioso para qualquer agente que pesquisa a web.

O Braim deve ter um `ResearchPolicy` com:

- timeout;
- concorrência;
- limite de páginas;
- robots/terms;
- deduplicação;
- tamanho máximo de resposta;
- origem da informação.

## Update skill

A Skill `geo-update` compara a instalação com o upstream e atualiza skills/agentes/scripts/schema. Isso inspira versionamento e atualização segura do catálogo de Skills do Braim.

## Relatórios

O projeto gera Markdown e PDF estruturados. O Braim pode separar `raw_result` de `deliverable`, permitindo que uma mesma execução produza diferentes formatos.

## O que absorver

- Skill principal + sub-Skills;
- agentes especialistas;
- pipeline discovery → parallel → synthesis;
- scoring;
- quality gates;
- comandos como capabilities;
- atualização/versionamento;
- outputs estruturados;
- relatórios;
- armazenamento persistente;
- limites de pesquisa.

## O que não absorver

- lógica específica de SEO/GEO como core;
- métricas de mercado sem validação;
- instalador automático sem auditoria;
- dados externos como fatos permanentes.

## Licença

O repositório usa MIT. Ainda assim, dependências e recursos individuais devem ser conferidos antes de cópia literal.

## Prioridade

**ALTA.** É um excelente modelo para o primeiro agente especialista real do Braim.

## Fontes

- https://github.com/zubair-trabzada/geo-seo-claude
- https://github.com/zubair-trabzada/geo-seo-claude/blob/main/geo/SKILL.md
- https://github.com/zubair-trabzada/geo-seo-claude/blob/main/docs/architecture.md
- https://github.com/zubair-trabzada/geo-seo-claude/blob/main/install.sh
- https://github.com/zubair-trabzada/geo-seo-claude/blob/main/LICENSE

## Conclusão

O aprendizado mais importante é a estrutura **um problema grande → descoberta → especialistas paralelos → síntese → score → plano de ação**. Isso deve ser um padrão nativo do Brain para tarefas complexas.
