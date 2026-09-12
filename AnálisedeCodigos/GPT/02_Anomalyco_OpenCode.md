# 02 — anomalyco/OpenCode

## Objetivo

Estudar como um coding agent organiza agentes, Skills, ferramentas, permissões, sessões, tarefas e configuração sem transformar o LLM no núcleo do sistema.

## O que mais interessa

OpenCode possui agentes com papéis diferentes. O `build` tem acesso amplo para desenvolvimento; o `plan` é orientado a leitura/análise e restringe alterações; existe também um subagente `general` para pesquisas complexas. Isso é extremamente compatível com o conceito do Braim de escolher especialistas conforme a tarefa.

## Skills

O loader procura diretórios contendo `SKILL.md`. A Skill é uma unidade de conhecimento operacional que pode ser carregada quando necessária. O Braim deve adotar a mesma ideia: não colocar todas as instruções de todos os domínios no prompt permanente.

Uma Skill do Braim deve conter:

- nome;
- descrição;
- gatilhos/capacidades;
- pré-requisitos;
- ferramentas necessárias;
- instruções;
- exemplos;
- critérios de sucesso;
- limitações;
- versão;
- origem/licença;
- score histórico.

## Agentes

A separação `build`/`plan` mostra que agente não é apenas modelo. Agente é uma configuração operacional: objetivo, permissões, ferramentas, modelo, comportamento e contexto.

Para o Braim:

```text
AgentProfile
  id
  role
  capabilities[]
  skills[]
  tools[]
  permissions
  preferred_models[]
  fallback_models[]
  quality_score
  cost_score
  latency_score
```

## Tarefas

OpenCode também possui ferramenta para listas de tarefas e progresso em operações multi-step. O Braim deve ter isso nativamente, porém com estado persistente e mais estruturado.

Uma tarefa deve registrar dependências, tentativas, agente escolhido, prompt, resultado e aprendizado.

## Permissões

A configuração do OpenCode trata agentes e permissões como elementos separados. Essa é uma boa arquitetura para o Braim: o agente pode saber fazer uma operação, mas ainda assim não possuir autorização para executá-la.

## Ferramentas

A documentação atual inclui ferramentas para carregar Skills e ferramentas customizadas. O princípio absorvível é um registry de tools com contrato estável, em vez de código especial espalhado pelo orquestrador.

## Comandos

A ideia de comandos slash é útil como interface humana, mas no Braim deve ser uma camada de entrada, não o mecanismo interno. O Brain deve transformar comandos em intenções estruturadas.

## Configuração

OpenCode trabalha com configuração para modelo, providers, agentes e permissões. O Braim deve separar:

1. configuração estática;
2. estado de execução;
3. catálogo dinâmico;
4. memória;
5. secrets.

Misturar essas cinco coisas dificulta aprendizado e recuperação.

## O que absorver

- Skill loader baseado em `SKILL.md`.
- AgentProfile independente do modelo.
- agentes de leitura/análise versus execução;
- ferramentas customizadas com schema;
- task list para trabalho multi-etapas;
- permissões por agente/ferramenta;
- configuração declarativa;
- seleção de modelo separada do papel do agente;
- subagente para pesquisa complexa;
- comandos humanos como camada de entrada.

## O que não copiar

- TUI/UI.
- Arquitetura inteira do OpenCode.
- Formato de configuração literalmente.
- Dependência de TypeScript/Bun como requisito do Brain.
- Modelo de produto.

## Aplicação no Braim

A arquitetura ideal fica:

```text
Intent
  ↓
Capability Resolver
  ↓
Skill Loader
  ↓
Agent Selector
  ↓
Tool Registry
  ↓
Execution
  ↓
Review
```

O ponto importante é que **Skill não é Agent e Agent não é Model**.

## Licença

OpenCode usa MIT. A licença permite reutilização, mas qualquer cópia literal deve preservar o aviso de copyright e licença.

## Prioridade

**CRÍTICA.** É a principal referência para Skills + agentes especializados + permissões.

## Fontes

- https://github.com/anomalyco/opencode
- https://github.com/anomalyco/opencode/blob/dev/README.md
- https://github.com/anomalyco/opencode/blob/dev/packages/web/src/content/docs/tools.mdx
- https://github.com/anomalyco/opencode/blob/dev/packages/core/src/plugin/skill/customize-opencode.md
- https://github.com/anomalyco/opencode/blob/dev/specs/v2/config.md
- https://github.com/anomalyco/opencode/blob/dev/LICENSE

## Conclusão

O maior aprendizado é separar **capacidade, Skill, agente, ferramenta e modelo**. Essa separação deve ser estrutural no Braim desde o primeiro commit, porque será a base para seleção automática e aprendizado posterior.
