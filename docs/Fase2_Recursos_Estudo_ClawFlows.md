# Fase 2 — Recurso de Estudo: ClawFlows

## Referência

- Repositório: `nikilster/clawflows`
- Projeto: **ClawFlows — Superpowers For Your Agent**
- Foco: workflows reutilizáveis para OpenClaw
- Licença: MIT
- Fonte: https://github.com/nikilster/clawflows

## Por que este projeto entra na Fase 2

O ClawFlows é especialmente relevante para o estudo da camada de **tarefas, workflows e automação persistente** que estamos planejando para o ecossistema IaBrain + SandBox + OpenClaw.

O projeto apresenta uma biblioteca de workflows prontos, versionados e reutilizáveis, com execução agendada, criação de workflows próprios, validação, logs, backup/restore e participação da comunidade.

A ideia importante para o nosso estudo não é copiar o OpenClaw nem transformar ClawFlows em parte obrigatória do SandBox. É entender como transformar **tarefas recorrentes e automações em unidades declarativas, reutilizáveis, versionáveis e observáveis**.

## O que estudar

### 1. Workflow como unidade reutilizável

Estudar a separação entre:

- agente;
- tarefa;
- workflow;
- schedule;
- execução;
- histórico;
- resultado;
- erro;
- logs.

Isso reforça a arquitetura planejada para a aba de tarefas do OpenClaw: poucos agentes especializados podem executar muitas tarefas independentes.

### 2. Workflows declarativos

O ClawFlows utiliza arquivos `WORKFLOW.md` com informações como agenda e instruções do workflow.

Estudar:

- formato declarativo;
- identificação do workflow;
- schedule em linguagem simples;
- instruções de execução;
- validação;
- versionamento;
- edição sem alterar o agente responsável.

### 3. Agendamento e execução recorrente

Estudar como o sistema transforma uma definição de workflow em execução automática:

`workflow → schedule → execução → resultado → histórico`

Isso é diretamente relacionado ao futuro sistema de tarefas persistentes do IaBrain/OpenClaw.

### 4. Biblioteca de workflows

O projeto disponibiliza mais de 100 workflows prontos, cobrindo áreas como:

- comunicação;
- produtividade;
- desenvolvimento;
- finanças;
- viagens;
- conteúdo;
- arquivos;
- segurança;
- sistema;
- automação residencial;
- tarefas recorrentes.

O catálogo deve ser estudado como exemplo de **biblioteca de capacidades reutilizáveis**, não como dependência obrigatória.

### 5. Workflows comunitários

O repositório possui uma área específica para submissões da comunidade.

Estudar:

- contribuição externa;
- revisão;
- validação antes da publicação;
- autoria/proveniência;
- versionamento;
- confiança no workflow;
- possibilidade de avaliar workflows antes de permitir execução.

Isso é importante para o futuro catálogo de Skills/APIs/Workflows do IaBrain.

### 6. CLI e ciclo de vida

Estudar os comandos conceituais oferecidos pelo projeto:

- listar workflows;
- listar habilitados/disponíveis;
- criar;
- executar;
- habilitar/desabilitar;
- editar;
- validar;
- consultar logs;
- atualizar catálogo;
- sincronizar informações do agente;
- backup/restore;
- submissão comunitária.

Essas operações formam uma referência útil para o futuro **Task Manager**.

### 7. Observabilidade

Estudar a existência de logs e histórico de execução.

Para IaBrain, cada tarefa deverá poder registrar:

- ID da tarefa;
- workflow utilizado;
- agente executor;
- início/fim;
- duração;
- status;
- resultado;
- erro;
- recursos utilizados;
- APIs/Skills utilizadas;
- custo quando aplicável;
- estratégia utilizada;
- tentativas/retries.

### 8. Versionamento e rollback

O projeto trata workflows como artefatos versionáveis e reutilizáveis.

Estudar como isso pode ser aplicado ao catálogo interno do IaBrain:

`workflow v1 → execução → avaliação → workflow v2 → comparação`

O sistema pode aprender quais versões produzem melhores resultados.

## Relação com IaBrain

O ClawFlows não deve substituir o IaBrain.

A arquitetura continua:

`Usuário → IaBrain → planejamento → seleção de recurso → tarefa → agente → SandBox → resultado → validação → memória`

O ClawFlows serve como **fonte de estudo para a camada de workflows/tarefas**.

O IaBrain deve continuar responsável por:

- decidir o que fazer;
- decompor objetivos;
- escolher agentes;
- escolher Skills/APIs/workflows;
- controlar permissões;
- controlar prioridades;
- controlar dependências;
- reagir a falhas;
- avaliar resultados;
- aprender com as execuções.

## Relação com o plano OpenClaw — Tarefas

Este recurso se conecta diretamente ao plano futuro:

- poucos agentes;
- muitas tarefas;
- tarefas independentes;
- schedules diferentes;
- execução recorrente;
- histórico por tarefa;
- recuperação após falhas;
- persistência;
- logs;
- capacidade/concurrency controlada pelo IaBrain.

Uma tarefa não precisa virar um novo agente.

Exemplo:

`Agente de Desenvolvimento`

pode executar:

- verificar PRs;
- executar testes;
- acompanhar CI;
- gerar APK;
- atualizar dependências;
- fazer auditoria semanal.

Cada item é uma **tarefa**, enquanto o agente é o executor especializado.

## Relação com o SandBox

O SandBox continua sendo o ambiente de execução:

`IaBrain decide → Workflow descreve → Agente executa → SandBox fornece ambiente → resultado retorna ao IaBrain`

O SandBox não precisa conhecer OpenClaw ou ClawFlows.

Ele fornece:

- processos;
- filesystem;
- ferramentas;
- Git;
- toolchains;
- jobs;
- logs;
- testes;
- segurança;
- artefatos;
- persistência necessária à execução.

## Relação com aprendizado

O ClawFlows pode servir como referência para o futuro sistema de aprendizado de experiências:

`Problema → Workflow → Agente → Skill/API → Execução → Resultado → Avaliação`

O IaBrain poderá registrar:

- frequência de uso;
- taxa de sucesso;
- duração;
- falhas;
- custo;
- qualidade;
- contexto em que funcionou;
- contexto em que falhou.

Assim, no futuro, a escolha de workflow deixa de ser apenas uma regra fixa e passa a considerar experiência histórica.

## Segurança e confiança

Workflows são automações executáveis e podem acessar dados, APIs, arquivos e sistemas externos. Portanto, um catálogo interno não deve executar automaticamente qualquer workflow descoberto na Internet.

Estudar mecanismos para:

- proveniência;
- autoria;
- licença;
- permissões necessárias;
- APIs utilizadas;
- escopo de acesso;
- risco;
- validação;
- sandboxing;
- histórico de comportamento;
- aprovação antes da execução.

Workflows comunitários devem ser tratados como recursos externos até serem avaliados.

## O que NÃO fazer

- Não copiar o ClawFlows inteiro para dentro do SandBox.
- Não transformar OpenClaw no cérebro do IaBrain.
- Não transformar workflows em agentes.
- Não executar automaticamente workflows externos sem validação.
- Não assumir que o formato do ClawFlows precisa ser o formato definitivo do IaBrain.
- Não criar dependência obrigatória do projeto externo.

## Estratégia de estudo

`estudar → comparar → prototipar → testar → medir → adaptar → decidir`

O objetivo é extrair os padrões arquiteturais úteis para o nosso próprio sistema.

## Princípio

> Não precisamos adotar o ClawFlows. Precisamos aprender como uma biblioteca de workflows pode transformar automações em capacidades reutilizáveis, versionáveis, agendáveis e observáveis — e então construir isso de forma nativa dentro da arquitetura IaBrain + OpenClaw + SandBox.
