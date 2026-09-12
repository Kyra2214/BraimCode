# Recurso de Estudo — Anthropic Agent Skills

## Referência

- Repositório: `anthropics/skills`
- Projeto: Anthropic Agent Skills
- Tipo: coleção de Agent Skills open source/demonstrativas
- Fonte: https://github.com/anthropics/skills

## O que é

O repositório apresenta Skills como pastas contendo instruções, scripts e recursos que um agente carrega dinamicamente para executar tarefas especializadas de forma repetível.

Cada Skill possui um `SKILL.md` com metadados e instruções. O repositório também contém exemplos técnicos, criativos, empresariais e de documentação, além da especificação e de um template para criação de Skills.

## Por que é importante para a Fase 2

Este é um dos recursos mais diretamente relacionados à arquitetura planejada do IaBrain.

O objetivo não é transformar o IaBrain em Claude nem depender da implementação da Anthropic. O objetivo é estudar o conceito de Skill como unidade reutilizável de capacidade.

## Modelo estudado

`Skill = instruções + recursos + scripts + contexto especializado`

A Skill pode ensinar um agente a executar uma determinada classe de tarefas sem transformar essa lógica em conhecimento permanente do cérebro.

## Relação com IaBrain

O IaBrain pode manter um catálogo próprio de Skills:

`problema → descoberta → Skill → agente → SandBox → resultado`

Cada Skill pode possuir:

- ID e nome;
- descrição;
- gatilhos/condições de uso;
- instruções;
- scripts;
- recursos de referência;
- dependências;
- ferramentas necessárias;
- APIs/Connectors utilizados;
- permissões;
- custo estimado;
- tempo médio;
- histórico de sucesso/falha;
- agentes compatíveis;
- versões;
- origem/proveniência;
- licença;
- nível de confiança.

## Skill x Agent

A distinção deve ser preservada:

- **Skill:** capacidade reutilizável.
- **Agent:** executor especializado.
- **Workflow:** sequência de Skills.
- **Connector:** integração com recurso externo.
- **API:** recurso/serviço externo.
- **SandBox:** ambiente onde o trabalho é executado.
- **IaBrain:** orquestrador que decide como combinar tudo.

Isso evita criar um agente novo para cada pequena capacidade.

## Skill dinâmica

Um ponto especialmente interessante é o carregamento sob demanda. O agente não precisa receber todas as instruções de todas as Skills em todos os trabalhos.

Isso combina com a arquitetura do IaBrain em que o contexto completo permanece no sistema, enquanto o LLM recebe apenas o contexto necessário para a etapa atual.

## Skills + aprendizado

As Skills também podem participar da rede de aprendizado:

`Problema → Skill → Prompt → Agent → Ferramentas → Resultado`

O IaBrain pode registrar:

- quantas vezes a Skill foi usada;
- em quais problemas funcionou;
- taxa de sucesso;
- erros encontrados;
- tempo de execução;
- custo;
- agente que executou;
- APIs/Connectors envolvidos;
- necessidade de revisão;
- alternativas que funcionaram melhor.

Com isso, o catálogo deixa de ser apenas uma lista e passa a representar experiência acumulada.

## Skill + SandBox

Scripts e ferramentas de uma Skill devem ser executados dentro das políticas do SandBox quando houver necessidade de execução.

Fluxo:

`IaBrain seleciona Skill → agente recebe Skill → agente usa SandBox → SandBox executa → resultado retorna → IaBrain valida`

A Skill não deve ganhar acesso irrestrito ao sistema apenas por estar cadastrada.

## Proveniência e segurança

O repositório da Anthropic deve ser tratado como fonte de estudo e inspiração. Skills externas precisam ser avaliadas antes de entrar no catálogo operacional.

Registrar:

- origem;
- autor;
- repositório;
- commit/versão;
- licença;
- arquivos incluídos;
- dependências;
- scripts executáveis;
- permissões solicitadas;
- acesso à rede;
- acesso a arquivos;
- comportamento observado;
- resultados de testes.

Skills não confiáveis podem permanecer apenas no laboratório de testes.

## Licenciamento

O README informa que muitas Skills do repositório são Apache 2.0, enquanto algumas Skills de criação/edição de documentos são source-available e não open source. Portanto, cada Skill deve ter sua licença/proveniência analisada individualmente antes de reutilização.

## Relação com Matt Pocock Skills

O `mattpocock/skills`, já registrado como recurso da Fase 2, pode ser estudado em conjunto com `anthropics/skills`.

A comparação deve buscar:

- estrutura de Skills;
- granularidade;
- metadados;
- instruções;
- scripts;
- recursos auxiliares;
- descoberta/roteamento;
- versionamento;
- testes;
- proveniência;
- segurança.

A meta é definir um formato próprio do IaBrain, preservando créditos/licenças quando houver reutilização legítima.

## Relação com OpenCode

O OpenCode já registrado na Fase 2 representa o coding agent/executor. Anthropic Agent Skills representa uma fonte forte para estudar como fornecer capacidades especializadas reutilizáveis ao agente.

`IaBrain → seleciona Skill → OpenCode/Agente → SandBox → execução`

OpenCode não precisa ser dependência do IaBrain; é apenas uma referência de agente de programação.

## O que NÃO fazer

- Não copiar o repositório inteiro para o IaBrain.
- Não assumir que toda Skill possui a mesma licença.
- Não executar scripts externos sem análise e isolamento.
- Não transformar Skills em agentes independentes.
- Não colocar a lógica de orquestração dentro das Skills.
- Não fazer o LLM decidir sozinho quais Skills têm permissão para executar ações sensíveis.
- Não criar dependência obrigatória da Anthropic.

## Objetivo de estudo

Extrair os conceitos úteis para criar um **Skill Engine nativo do IaBrain**, com catálogo, descoberta, versionamento, segurança, execução controlada, avaliação e aprendizado.

## Princípio

**Skill é capacidade. Agent é executor. Workflow é composição. SandBox é execução. IaBrain é decisão.**
