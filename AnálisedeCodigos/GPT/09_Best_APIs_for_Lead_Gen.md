# 09 — cporter202/best-apis-for-lead-gen

## Objetivo

Estudar como organizar um catálogo de APIs por capacidade, workflow, risco, qualidade e utilidade prática.

## Estrutura encontrada

O repositório separa:

- `apis/` — perfis individuais;
- `workflows/` — combinações de APIs;
- `playbooks/` — produtos/serviços que podem ser construídos;
- `categories/` — indexação por caso de uso;
- `templates/` — padrão para adicionar novos recursos;
- `resources/` — critérios e estratégias.

Essa estrutura é extremamente compatível com o catálogo do Braim.

## Ideia mais importante: API não é apenas endpoint

Cada API possui contexto de uso, strengths, weaknesses, workflow fit, preço e riscos. O catálogo do Braim deve armazenar mais que URL.

## Schema sugerido

```text
ApiCapability
  provider
  product
  endpoint
  capabilities[]
  categories[]
  auth_type
  pricing
  free_tier
  limits
  latency
  quality_score
  reliability_score
  legal_notes
  terms_url
  documentation_url
  last_verified
  source
```

## Waterfall

O repositório possui workflows de enriquecimento em cascata. Isso confirma a arquitetura de fallback do Braim:

```text
Provider A
  ↓ falhou/incompleto
Provider B
  ↓ falhou/incompleto
Provider C
  ↓
resultado final
```

Não basta trocar API quando ocorre erro HTTP. O Brain deve detectar resultado incompleto e qualidade insuficiente.

## Workflows

Os workflows mostram como transformar várias APIs em uma capacidade maior. O Braim deve aprender a montar pipelines compostos.

Exemplo:

```text
Pesquisa local
→ descoberta
→ enriquecimento
→ validação
→ scoring
→ relatório
```

## Curation score

A grande lição é preferir poucos recursos úteis a um dump gigantesco. O catálogo do Braim deve ter status:

- descoberto;
- não validado;
- validado;
- recomendado;
- degradado;
- bloqueado;
- expirado.

## Affiliate/neutralidade

O repositório preserva links de afiliados em algumas fontes. O Brain não deve assumir que link afiliado significa qualidade.

## O que absorver

- catálogo por capacidade;
- categorias;
- perfis de API;
- workflows de combinação;
- templates;
- waterfall/fallback;
- critérios de seleção;
- compliance notes;
- atualização do catálogo.

## O que não absorver

- URLs afiliadas como fonte de confiança;
- qualquer API como dependência fixa;
- catálogo estático sem verificação.

## Prioridade

**ALTA** para o “minerador de APIs gratuitas”.

## Fonte

https://github.com/cporter202/best-apis-for-lead-gen

## Conclusão

Esse repositório fornece quase diretamente o modelo do **catálogo inteligente de recursos** do Braim. O nosso catálogo será ainda melhor porque terá quota dinâmica, histórico de sucesso e custo zero como critério principal.
