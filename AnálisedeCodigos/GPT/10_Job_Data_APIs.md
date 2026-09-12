# 10 — cporter202/job-data-apis-and-scrapers

## Objetivo

Estudar descoberta de fontes, sincronização automática de catálogo, seleção de provider e normalização de dados.

## O que mais interessa

O repositório mantém catálogos de APIs/actors para dados de vagas e hiring signals. O catálogo possui manutenção automática por GitHub Actions e sincronização diária.

## Lição principal: catálogo vivo

O catálogo de APIs do Braim não deve ser um arquivo congelado. Recursos mudam preço, disponibilidade, limite, endpoint e qualidade.

Precisamos de um processo:

```text
Discovery
→ Fetch metadata
→ Validate
→ Test
→ Compare
→ Update catalog
```

## Provider selection checklist

O repositório recomenda comparar:

- cobertura;
- país/idioma;
- frescor;
- paginação;
- qualidade dos campos;
- deduplicação;
- formato estruturado;
- API/MCP;
- rate limits;
- preço;
- termos.

Essa checklist deve virar parte do `ApiEvaluator` do Braim.

## Sync

Existe comando local `node settings/sync_catalog.js` e automação diária. O Brain pode ter um `ResourceMiner` que roda periodicamente quando houver servidor persistente.

## Normalização

A ideia de normalizar dados de múltiplas fontes é fundamental. O Brain deve transformar respostas diferentes em um schema interno antes de avaliar qualidade.

## O que absorver

- catálogo sincronizado;
- verificação diária;
- provider selection checklist;
- comparação de cobertura;
- avaliação de frescor;
- deduplicação;
- normalização;
- classificação por caso de uso;
- automação de atualização.

## Aplicação além de APIs de emprego

O mesmo mecanismo serve para:

- APIs de IA;
- APIs de imagem;
- APIs de vídeo;
- APIs de música;
- APIs de voz;
- APIs de pesquisa;
- scrapers;
- ferramentas locais.

## Cuidado

O repositório preserva parâmetros de referral existentes. Isso não deve ser tratado como recomendação independente.

## Prioridade

**ALTA** para o sistema “Minerar API Gratuita”.

## Fonte

https://github.com/cporter202/job-data-apis-and-scrapers

## Conclusão

A principal absorção é o conceito de **catálogo vivo + avaliação objetiva + sincronização automática**. Isso é mais importante para o Braim que a lista específica de APIs de emprego.
