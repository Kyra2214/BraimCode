# Fase 2 — Recurso de Estudo: Job Data APIs and Scrapers

## Referência

Projeto: `cporter202/job-data-apis-and-scrapers`

Descrição: diretório focado em APIs e scrapers para vagas, sinais de contratação, salários, pesquisa de empregadores e workflows de recrutamento.

## Por que estudar na Fase 2

Este catálogo amplia a camada de descoberta do IaBrain para dados de mercado de trabalho e inteligência empresarial.

O repositório organiza recursos para:

- agregação de vagas;
- dados de job boards;
- sinais de contratação;
- salários e skills;
- pesquisa de empresas/empregadores;
- recrutamento;
- análise de mercado de trabalho;
- dashboards e alertas.

O README informa que o diretório é sincronizado diariamente e atualmente organiza catálogos de Apify e CoreClaw. Os números são dinâmicos e devem ser tratados como dados temporais, não como valores fixos. citeturn0search0

## Pontos para estudar

- catálogo de APIs por capacidade;
- descoberta de recursos por objetivo;
- agregação de múltiplos job boards;
- normalização e deduplicação de vagas;
- sinais derivados de atividade de contratação;
- dados de salário, localização e skills;
- enriquecimento de empresas;
- seleção de provedores;
- comparação de custo, cobertura e limitações;
- atualização automática de catálogos;
- registro da origem dos dados;
- avaliação de confiabilidade dos provedores.

## Relação com o IaBrain

O principal aprendizado não é criar um sistema de recrutamento. É estudar como o IaBrain pode transformar um catálogo especializado em conhecimento operacional pesquisável.

Fluxo a avaliar:

`Objetivo → capacidade necessária → catálogo → recursos candidatos → custo/limitações → validação → escolha → execução → resultado → avaliação`

Exemplo:

`"Quero analisar vagas de Android no Brasil" → job discovery → candidatos de API/scraper → cobertura/custo → coleta → normalização → análise → resultado`

## Relação com a rede de aprendizado

Cada execução poderá futuramente registrar:

- objetivo;
- API/scraper escolhido;
- fonte/provedor;
- parâmetros utilizados;
- tempo;
- custo;
- volume de dados;
- cobertura;
- erros;
- qualidade dos dados;
- necessidade de normalização;
- duplicidades encontradas;
- resultado final.

Isso alimenta a relação:

`Problema → Fonte → API → Estratégia → Skill → Prompt → Agente → Resultado → Avaliação`

O IaBrain poderá aprender quais recursos funcionam melhor para cada tipo de pesquisa.

## Relação com o catálogo de APIs

Este projeto deve ser tratado como **fonte de descoberta**, alimentando o catálogo interno do IaBrain.

Não deve ser transformado automaticamente em dependência estrutural.

O catálogo interno deve preferencialmente guardar metadados como:

- nome;
- provedor real;
- capacidade;
- entradas/saídas;
- API/MCP quando aplicável;
- autenticação;
- custo;
- limites;
- cobertura geográfica;
- termos de uso;
- documentação oficial;
- licença;
- origem da descoberta;
- data da descoberta;
- evidências de funcionamento;
- confiabilidade histórica;
- histórico de sucesso/falha;
- eventual rastreamento comercial/afiliado.

## Relação com outros recursos cporter202

O catálogo complementa outros recursos já estudados na Fase 2, especialmente catálogos gerais de APIs, Agentic AI, OpenClaw/CoreClaw e scraping.

Também existe uma categoria de `jobs-apis` no `openclaw-api-list`, reforçando a ideia de tratar recursos de emprego como uma capacidade especializada dentro do catálogo maior. citeturn0search1

## Cuidados

O README do projeto alerta para respeitar termos dos job boards, regras de robots, privacidade, regulamentações relacionadas a dados de emprego, limites de requisição e restrições de profiling automatizado.

Portanto, qualquer uso futuro deverá passar por:

`descoberta → validação jurídica/técnica → credenciais/permissões → teste → execução controlada → avaliação`

Nunca assumir que um scraper disponível no catálogo pode ser usado livremente em qualquer contexto.

## O que NÃO fazer agora

- não incorporar o catálogo inteiro ao RootFS;
- não instalar automaticamente todos os scrapers;
- não transformar Apify/CoreClaw em dependência obrigatória;
- não copiar código de terceiros sem revisar licença e créditos;
- não tratar links de catálogo como recomendação de qualidade;
- não executar scraping sem verificar termos, privacidade e limites;
- não assumir que a quantidade de APIs representa qualidade.

Primeiro: **estudar → catalogar → validar → testar → medir → decidir**.

## Resultado esperado da Fase 2

A ideia útil a extrair é uma capacidade própria do IaBrain para descoberta e seleção de **recursos de dados especializados por domínio**.

O padrão pode ser generalizado para:

- empregos;
- ecommerce;
- imóveis;
- redes sociais;
- vídeos;
- notícias;
- leads;
- pesquisa de mercado;
- outros domínios.

### Princípio de estudo

> **Não precisamos adotar o catálogo. Precisamos aprender a transformar catálogos especializados em conhecimento próprio, pesquisável, validado e útil para o IaBrain.**
