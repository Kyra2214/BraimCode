# Fase 2 — Recurso de Estudo: geo-seo-claude

## Referência

- Repositório: `zubair-trabzada/geo-seo-claude`
- Autor/conta: `zubair-trabzada`
- Tema: GEO (Generative Engine Optimization) + SEO para busca alimentada por IA
- Licença declarada: MIT
- URL: https://github.com/zubair-trabzada/geo-seo-claude

## Por que este projeto entra no estudo do SandBox / IaBrain

O `geo-seo-claude` é uma referência prática para transformar GEO em capacidades reutilizáveis de auditoria, análise, geração e acompanhamento. O projeto é orientado a Claude Code, mas seus conceitos podem ser estudados independentemente da plataforma.

O foco do estudo não é incorporar Claude Code nem transformar o projeto em dependência do SandBox. O objetivo é identificar capacidades que possam futuramente virar Skills, Tools, workflows ou agentes especialistas nativos dentro da arquitetura IaBrain + SandBox.

## Capacidades observadas

O README apresenta uma arquitetura com um skill principal e múltiplos sub-skills especializados, cinco subagentes paralelos, scripts Python e templates JSON-LD.

### Auditoria GEO + SEO

Fluxo descrito pelo projeto:

1. descoberta da página e sitemap;
2. identificação do tipo de negócio;
3. análise paralela por especialistas;
4. síntese dos resultados;
5. geração de GEO Score de 0–100;
6. plano de ação priorizado.

### Subcapacidades

- citabilidade e preparação para citações por IA;
- análise de crawlers de IA via `robots.txt`;
- análise/geração de `llms.txt`;
- presença e menções de marca;
- otimização específica por plataforma de busca de IA;
- análise e geração de Schema/JSON-LD;
- fundamentos técnicos de SEO;
- qualidade de conteúdo e E-E-A-T;
- relatórios Markdown/PDF;
- prospecção de clientes;
- geração de propostas;
- comparação mensal e acompanhamento de evolução.

## Arquitetura interessante para estudo

O projeto separa:

- **orquestração** — skill principal `geo/SKILL.md`;
- **skills especializadas** — módulos independentes para cada capacidade;
- **agentes especialistas** — cinco áreas executadas em paralelo;
- **scripts** — fetching, scoring, brand scanning, llms.txt e PDF;
- **templates de schema** — modelos reutilizáveis de dados estruturados;
- **dados persistentes** — prospects, propostas e relatórios em `~/.geo-prospects/`.

Essa separação é particularmente útil para estudar como uma tarefa grande pode ser decomposta em capacidades menores e depois sintetizada por um orquestrador.

## Relação com IaBrain

A ideia não deve ser copiada como arquitetura central. O conceito pode ser adaptado para o modelo do IaBrain:

`Problema → descoberta → plano → skills → agentes → SandBox → resultados → validação → memória`

Exemplo futuro:

`"Analise o site X para melhorar presença em buscas de IA"`

O IaBrain poderia:

1. identificar que o problema pertence ao domínio GEO/SEO;
2. consultar seu catálogo de Skills/APIs/agentes;
3. selecionar uma estratégia de auditoria;
4. decompor o trabalho em funcionalidades;
5. enviar execução para agentes especialistas;
6. usar o SandBox para fetch, parsing, scripts, testes e geração de artefatos;
7. consolidar os resultados;
8. validar as recomendações;
9. armazenar o resultado e a experiência para futuras auditorias.

## Relação com o catálogo de Skills

As capacidades do projeto podem servir como referências para uma futura biblioteca nativa de Skills, por exemplo:

- `geo-audit`
- `geo-citability`
- `geo-crawlers`
- `geo-llmstxt`
- `geo-brand-mentions`
- `geo-platform-optimizer`
- `geo-schema`
- `geo-technical`
- `geo-content`
- `geo-report`
- `geo-compare`

A implementação nativa deve ser independente do Claude Code e obedecer à separação do IaBrain: o IaBrain decide; agentes executam; SandBox fornece o ambiente.

## Relação com OpenClaw / Tarefas

Algumas capacidades também podem se encaixar posteriormente como tarefas recorrentes:

- auditoria GEO mensal;
- comparação de score antes/depois;
- monitoramento de robots.txt e crawlers;
- atualização de `llms.txt`;
- acompanhamento de marca;
- geração periódica de relatório;
- alertas quando uma alteração reduzir a preparação para busca de IA.

Isso combina com o modelo futuro de **poucos agentes + muitas tarefas**, em que uma mesma especialidade pode executar dezenas de trabalhos isolados.

## Relação com aprendizado

O projeto é útil como fonte de estratégias que o IaBrain pode avaliar empiricamente.

A rede de aprendizado pode registrar:

`Site → tipo → problema → estratégia → Skill → ferramenta → agente → resultado → custo → tempo → qualidade → erro → correção`

Com isso, o sistema pode aprender quais verificações realmente produzem valor, quais estratégias geram melhores resultados e quais ferramentas são mais confiáveis.

## Pontos técnicos para estudar

### 1. Scoring

Estudar como transformar sinais heterogêneos em um score composto sem tratar o número como verdade absoluta.

### 2. Citabilidade

Estudar critérios para identificar trechos autocontidos, factuais e adequados para serem utilizados como citações por sistemas de IA.

### 3. Crawlers

Estudar parsing de `robots.txt`, identificação de crawlers e separação entre acesso técnico e preferências de uso do conteúdo.

### 4. Structured Data

Estudar geração e validação de JSON-LD e como schemas ajudam sistemas automatizados a compreender entidades e páginas.

### 5. Multi-agent analysis

Estudar a divisão de uma auditoria em especialistas independentes e posterior síntese.

### 6. Relatórios

Estudar geração de artefatos úteis para humanos, incluindo Markdown, PDF, gráficos, score breakdown e planos priorizados.

### 7. Histórico

Estudar comparação mensal e armazenamento de baseline para medir evolução ao longo do tempo.

## Pontos de atenção

- O projeto é construído especificamente para Claude Code.
- Não devemos tornar Claude Code uma dependência do SandBox.
- Não devemos transformar o projeto externo em parte obrigatória do IaBrain.
- Scores devem ser tratados como métricas heurísticas, não como garantias de posicionamento ou citação.
- Métricas de mercado, tráfego e correlação apresentadas no README devem ser verificadas antes de serem usadas como fatos internos.
- O conteúdo do projeto deve ser estudado respeitando a licença MIT e mantendo atribuição quando houver reutilização de código ou conteúdo coberto pela licença.
- Ferramentas externas, APIs e fontes devem entrar no catálogo do IaBrain com avaliação de confiabilidade, custo, disponibilidade, licença e histórico de resultados.

## Relação com os outros recursos da Fase 2

Este projeto complementa:

- `agentic-ai-apis` — descoberta de APIs e capacidades;
- `agentic-ai-starters` — arquiteturas e padrões de agentes;
- `agentic-ai-tools` / `ai-agent-tools` — ferramentas para agentes;
- `software-income-playbooks` — transformação de capacidades em produtos e serviços;
- `LangChain` — estudo de ferramentas, agentes, workflows e integrações;
- `ClawFlows` — workflows reutilizáveis, versionáveis e agendáveis;
- `OFFPack` — execução e cache offline;
- `Job Data APIs and Scrapers` — catálogos, seleção e avaliação de APIs/dados.

O GEO-SEO Claude acrescenta principalmente a dimensão **auditoria especializada + decomposição + scoring + histórico + relatório**.

## O que NÃO fazer

- não copiar o repositório inteiro para o SandBox;
- não fazer Claude Code virar o cérebro do IaBrain;
- não criar uma dependência obrigatória do repositório externo;
- não assumir que todos os scores ou métricas são universalmente corretos;
- não executar alterações em sites reais sem validação e permissões adequadas;
- não misturar a camada de GEO com a responsabilidade central do orquestrador.

## Próximo estudo recomendado

Aplicar o princípio:

**estudar → decompor → comparar → implementar uma versão mínima nativa → testar → medir → decidir**

O objetivo é extrair as ideias úteis para uma futura capacidade GEO do IaBrain/SandBox, sem acoplar a arquitetura a um projeto externo.

## Princípio

> Não precisamos adotar o `geo-seo-claude`. Precisamos aprender como uma auditoria GEO especializada pode ser decomposta em Skills, agentes, ferramentas, métricas, histórico e workflows — e então transformar as ideias úteis em capacidades nativas do ecossistema IaBrain + SandBox.
