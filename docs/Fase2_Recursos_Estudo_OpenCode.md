# Recurso de Estudo — OpenCode

## Referência

- Repositório: `anomalyco/opencode`
- Projeto: OpenCode
- Tipo: agente de programação open source
- Licença: MIT
- Fonte: https://github.com/anomalyco/opencode

## O que é

OpenCode é um agente de programação open source voltado para trabalhar com código a partir do terminal, com interface também disponível para desktop e IDE. O projeto possui uma arquitetura própria de agente, ferramentas e sessões, e suporta múltiplos modelos/provedores.

## O que estudar

### 1. Coding Agent

Estudar como o OpenCode transforma uma solicitação em trabalho concreto sobre um projeto:

`pedido → contexto → agente → ferramentas → código → testes → resultado`

Isso é diretamente relevante para os agentes especialistas do IaBrain.

### 2. Agentes especializados

O projeto possui agentes com papéis diferentes:

- `build`: execução completa de desenvolvimento;
- `plan`: análise e planejamento em modo somente leitura;
- `general`: subagente para pesquisas e tarefas complexas.

A separação é interessante para o modelo do IaBrain, mas não deve substituir o IaBrain como orquestrador central.

### 3. SandBox

A principal relação com o SandBox é a separação entre inteligência e ambiente de execução:

`IaBrain → agente → SandBox → ferramentas → projeto → testes → resultado`

OpenCode demonstra o lado do coding agent; SandBox fornece o ambiente persistente e controlado onde agentes podem trabalhar.

### 4. Multi-session / concorrência

Estudar a forma como sessões e múltiplos agentes podem trabalhar sobre projetos. Isso pode alimentar posteriormente o modelo do OpenClaw Tasks, em que poucos agentes atendem muitas tarefas independentes.

### 5. Ferramentas e permissões

Estudar:

- execução de comandos;
- leitura e alteração de arquivos;
- permissões;
- modos somente leitura;
- aprovação de ações;
- uso de subagentes;
- integração com ferramentas externas.

Esses conceitos são úteis para o Permission Layer e o Task Engine do IaBrain.

### 6. LSP e contexto de código

OpenCode utiliza LSP para fornecer inteligência sobre código. É uma referência importante para uma futura Skill de análise de código do IaBrain.

### 7. MCP e conectores

O projeto disponibiliza integração com MCP tools. Isso deve ser estudado como referência para o Connector Catalog do IaBrain:

`Connector → ferramenta externa → capacidade → Skill → Workflow`

Não significa adotar MCP como dependência obrigatória.

### 8. Modelos locais e provedores

OpenCode trabalha com múltiplos provedores e também permite modelos locais/self-hosted. Isso reforça a ideia de manter o modelo de linguagem substituível:

`IaBrain → Provider Gateway → LLM`

O LLM continua sendo secretário/analista quando essa arquitetura for aplicada ao IaBrain, e não o cérebro do sistema.

## Relação com os projetos

### IaBrain

OpenCode deve ser estudado como referência de **coding agent**, não como cérebro do sistema.

`IaBrain = Orquestração`

`OpenCode = referência de agente de programação`

`SandBox = ambiente de execução`

### SandBox

O SandBox pode fornecer ao agente:

- workspace;
- Git;
- toolchains;
- Android SDK;
- testes;
- builds;
- logs;
- artefatos;
- jobs;
- persistência;
- diagnóstico.

OpenCode pode servir como referência de como um agente consome esse ambiente.

### OpenClaw Tasks

A ideia de poucos agentes atendendo muitas tarefas pode aproveitar conceitos de sessões, subagentes, permissões e execução de tarefas do OpenCode.

## Fluxo de estudo

`problema → planejamento → agente → ferramentas → SandBox → código → teste → revisão → resultado → IaBrain`

O objetivo é estudar como tornar esse fluxo confiável, observável e recuperável.

## O que NÃO fazer

- Não colocar OpenCode como cérebro do IaBrain.
- Não transformar o SandBox em um wrapper do OpenCode.
- Não copiar código sem análise da licença e da arquitetura.
- Não assumir que o modelo de agentes do OpenCode serve integralmente para o IaBrain.
- Não criar dependência obrigatória do projeto externo.

## Princípio

O objetivo não é reproduzir o OpenCode. É aprender o que um coding agent moderno faz bem e transformar os conceitos úteis em capacidades nativas do ecossistema:

**IaBrain pensa e orquestra. Agentes trabalham. SandBox executa.**
