# Fase 2 — Recurso de Estudo: n8n

## Referência

- Repositório: `n8n-io/n8n`
- Projeto: n8n
- Tema: workflow automation, integrações, agentes e automação visual
- Licença: Sustainable Use License + n8n Enterprise License
- URL: https://github.com/n8n-io/n8n

## Por que estudar n8n

O n8n é uma referência forte para entender como transformar milhares de integrações e operações em workflows reutilizáveis e visualmente compostos. O README atual descreve uma plataforma de automação com 1500+ integrações, 9000+ templates de workflows, suporte a agentes/AI workflows, código customizado, aprovações humanas e observabilidade. citeturn0search0turn0search1

Para o ecossistema IaBrain + SandBox, o valor principal não é adotar n8n como cérebro. É estudar como uma grande camada de integrações pode ser catalogada, conectada e executada através de workflows.

## Conceitos para estudar

### 1. Integrações como capacidades

Cada integração representa uma capacidade potencial:

`serviço → autenticação → operações → entradas → saídas → limites → custo → confiabilidade`

Isso conversa diretamente com o Connector Catalog planejado para o IaBrain.

### 2. Workflow como unidade reutilizável

Um workflow pode representar uma sequência de ações:

`trigger → transformação → decisão → chamada de serviço → validação → saída`

Isso complementa o modelo de tarefas do OpenClaw/Agents e os workflows estudados no ClawFlows.

### 3. Composição visual

Estudar a ideia de representar processos complexos como blocos conectados, permitindo visualizar dependências e fluxo de dados.

### 4. Código quando necessário

O n8n combina construção visual com JavaScript, Python e pacotes npm para casos em que o bloco pronto não é suficiente. fileciteturn157file0

No SandBox isso pode ser traduzido para workflows que usam scripts, CLIs e ferramentas reais em vez de tentar colocar toda lógica dentro do orquestrador.

### 5. Human-in-the-loop

O projeto destaca workflows com aprovações humanas. fileciteturn157file0

Esse conceito é importante para o IaBrain: ações sensíveis podem exigir aprovação antes da execução, sem bloquear tarefas totalmente automáticas.

### 6. Observabilidade

Workflow não deve ser apenas uma sequência de chamadas. É necessário registrar:

- execução;
- duração;
- entradas/saídas relevantes;
- erros;
- retries;
- custo;
- resultado;
- aprovação humana;
- versão do workflow.

Isso alimenta a memória e a rede de aprendizado do IaBrain.

## Relação com IaBrain

A arquitetura desejada não é:

`IaBrain → n8n → tudo`

Mas sim:

`IaBrain → catálogo de capacidades → escolhe estratégia → cria/decompõe workflow → SandBox executa → resultados → validação → memória`

O n8n pode ser estudado como referência externa para a camada de workflow e integração.

O IaBrain continua sendo responsável por:

- planejamento;
- seleção de recursos;
- estado;
- memória;
- permissões;
- agentes;
- avaliação;
- recuperação de falhas;
- decisão final.

## Relação com Connector Catalog

O modelo de integração do n8n reforça a necessidade de um catálogo próprio no IaBrain.

Cada Connector poderia armazenar:

- ID;
- provedor;
- capacidades;
- operações;
- autenticação;
- permissões;
- dependências;
- protocolo;
- custo;
- limites;
- versão;
- disponibilidade;
- fonte;
- licença;
- confiabilidade;
- histórico de sucesso/falha;
- Skills relacionadas;
- workflows que utilizam o connector.

A diferença fundamental é que o IaBrain deve ser capaz de escolher entre **múltiplos fornecedores e estratégias**, e não ficar preso a uma plataforma de automação.

## Relação com SandBox

O SandBox é o ambiente de execução.

Um workflow poderia chamar:

- Git;
- Node;
- Python;
- Android SDK;
- banco de dados;
- HTTP APIs;
- scripts;
- testes;
- builds;
- ferramentas de análise;
- geração de artefatos.

O workflow descreve **o que fazer**; o SandBox fornece **onde e com quais ferramentas executar**.

## Relação com OpenClaw — Tarefas

O modelo de tarefas planejado pode utilizar workflows como implementação interna:

`Task → Workflow → Agents/Tools → SandBox → Result`

Uma mesma tarefa pode ter um workflow versionado e repetível. Muitas tarefas podem reutilizar o mesmo workflow sem criar um novo agente para cada uma.

Isso reforça o princípio:

**Poucos agentes. Muitas tarefas. Muitos workflows reutilizáveis.**

## Relação com ClawFlows

ClawFlows já foi adicionado como recurso de estudo da Fase 2 e está mais focado na biblioteca de workflows reutilizáveis para OpenClaw.

O n8n acrescenta uma perspectiva diferente:

- grande catálogo de integrações;
- composição visual;
- execução de workflows;
- extensibilidade;
- integração com código;
- automação de processos reais;
- observabilidade.

ClawFlows ajuda a estudar **biblioteca/lifecycle de workflows**; n8n ajuda a estudar **engine/ecossistema de workflows e integrações**.

## Relação com APIs e Skills

O catálogo de APIs responde:

> "Que recurso externo existe?"

O catálogo de Connectors responde:

> "Como podemos conversar com esse recurso?"

A Skill responde:

> "Como realizar determinada capacidade?"

O Workflow responde:

> "Em que sequência essas capacidades devem ser executadas?"

O Agente responde:

> "Quem executa e resolve problemas durante o processo?"

O SandBox responde:

> "Onde a execução acontece?"

E o IaBrain responde:

> "Qual estratégia usar e por quê?"

## Possível arquitetura nativa futura

Uma versão própria e desacoplada poderia ter:

`Workflow Definition`
→ `Trigger`
→ `Planner`
→ `Connector Resolver`
→ `Skill Executor`
→ `Sandbox Job`
→ `Validator`
→ `Artifact`
→ `Memory/Learning`

Cada execução teria ID, estado, logs, duração, resultados e evidências.

## Aprendizado

O IaBrain poderia aprender quais workflows são mais eficientes:

`problema → workflow → connectors → ferramentas → agente → execução → resultado → custo → tempo → qualidade → falhas`

Com histórico suficiente, pode descobrir:

- qual workflow funciona melhor para determinado problema;
- quais connectors são mais confiáveis;
- quais APIs são mais baratas;
- onde ocorrem falhas;
- quando inserir aprovação humana;
- quais etapas podem ser paralelizadas;
- quais workflows podem ser transformados em Skills.

## Segurança

Integrações são uma superfície de risco importante.

O estudo deve considerar:

- menor privilégio;
- secrets fora de logs;
- permissões por connector;
- aprovação para operações destrutivas;
- isolamento de execução;
- validação de entradas;
- limites de chamadas;
- auditoria;
- rollback quando possível;
- provenance dos dados e artefatos.

## Licença e adoção

O n8n não deve ser copiado ou incorporado automaticamente ao SandBox. O projeto usa um modelo de licença próprio (Sustainable Use License e Enterprise License), portanto qualquer reutilização de código precisa passar por uma análise específica de licença. fileciteturn157file0

A intenção aqui é **estudar arquitetura, conceitos e padrões**, não importar código indiscriminadamente.

## O que NÃO fazer

- não transformar n8n no cérebro do IaBrain;
- não criar dependência obrigatória do n8n;
- não copiar o código do n8n sem análise de licença;
- não substituir o Connector Catalog próprio por um catálogo externo;
- não confundir workflow com agente;
- não criar um agente para cada workflow;
- não executar connectors sem permissões explícitas;
- não assumir que 1500+ integrações significam 1500 integrações automaticamente disponíveis no nosso ambiente.

## Próximo estudo recomendado

Seguir:

**estudar → mapear conceitos → comparar com ClawFlows → modelar Connector Catalog → modelar Workflow Engine → implementar mínimo nativo → testar → medir → decidir**

## Princípio

> Não precisamos adotar o n8n. Precisamos aprender como uma enorme camada de integrações, workflows reutilizáveis, execução visual, código, aprovação humana e observabilidade pode ser organizada — e transformar as ideias úteis em capacidades próprias do IaBrain + SandBox.
