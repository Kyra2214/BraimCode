# Fase 2 — Recurso de Estudo: OpenAI Codex CLI

## 1. Referência

- Repositório: `openai/codex`
- Projeto: Codex CLI
- Organização: OpenAI
- Licença: Apache-2.0
- Fonte: https://github.com/openai/codex

O Codex CLI é um coding agent que roda localmente no computador e opera pelo terminal. O repositório oficial disponibiliza binários para Linux x86_64 e ARM64, macOS e Windows, além de instalação via npm e Homebrew.

## 2. Por que estudar para o SandBox

O Codex é uma referência direta para o problema que o SandBox resolve: transformar um ambiente de terminal em um espaço de trabalho onde um agente consegue inspecionar código, executar comandos, modificar projetos, testar e produzir resultados.

A diferença arquitetural é importante: o SandBox não deve virar um clone do Codex nem possuir uma IA própria. Ele deve continuar sendo o ambiente de execução controlado pelo IaBrain e pelos agentes.

## 3. O que estudar no Codex

### 3.1 Coding agent no terminal

Estudar como um agente de programação é apresentado e operado a partir de uma interface de terminal, mantendo o ambiente de trabalho separado da inteligência que decide o que fazer.

### 3.2 Runtime local

Estudar a relação entre agente, filesystem, processos, comandos e projeto local. Isso é diretamente relacionado ao Runtime do SandBox.

### 3.3 Execução de código

Estudar mecanismos de execução, isolamento, captura de resultados, erros e estados de execução. No SandBox isso se conecta a `sandbox-run`, `sandbox-job`, logs, artifacts e diagnósticos.

### 3.4 Fluxo de desenvolvimento

Estudar o ciclo:

`entender → planejar → editar → executar → testar → corrigir → validar`

Esse ciclo pode ser convertido em capacidades nativas de agentes trabalhando dentro do SandBox.

### 3.5 Integração com ferramentas

Estudar como um coding agent utiliza ferramentas do ambiente em vez de tentar resolver tudo apenas pelo modelo.

Isso reforça a arquitetura:

`IaBrain → Agent → SandBox → Tools → Result`

## 4. Relação com IaBrain

Codex não deve substituir o IaBrain.

No projeto do usuário:

- IaBrain decide.
- LLM atua como secretário/analista quando necessário.
- IaBrain escolhe estratégia, Skill, API, Connector e agente.
- Agentes executam tarefas.
- SandBox fornece o ambiente.
- Testes e resultados retornam ao IaBrain.
- IaBrain integra, valida e aprende.

O Codex deve ser estudado principalmente como referência de **coding agent/executor**, não como cérebro do sistema.

## 5. Relação com SandBox

O principal aprendizado é a fronteira entre inteligência e ambiente.

### Codex

`Coding Agent → Terminal/Workspace → Código → Execução → Resultado`

### Arquitetura SandBox + IaBrain

`IaBrain → Agent → SandBox → Workspace/Runtime/Tools → Execução → Testes → Resultado → IaBrain`

Isso permite que diferentes agentes usem o mesmo ambiente sem duplicar infraestrutura.

## 6. Relação com agentes especialistas

Um agente de desenvolvimento pode usar o SandBox para:

- clonar e atualizar repositórios;
- analisar código;
- criar ou alterar arquivos;
- executar builds;
- executar testes;
- analisar logs;
- gerar APKs;
- validar artefatos;
- corrigir falhas;
- repetir ciclos de teste;
- devolver evidências ao IaBrain.

O conhecimento útil do Codex deve ser transformado em capacidades/Skills próprias, sem tornar o Codex uma dependência estrutural do SandBox.

## 7. Relação com RootFS

O RootFS já fornece grande parte da infraestrutura necessária para um coding agent:

- Git/Git LFS/GitHub CLI;
- Python;
- Node.js;
- Java/Gradle;
- C/C++;
- Rust;
- Go;
- CMake/Ninja;
- testes e diagnóstico;
- ferramentas Android no perfil Android/API;
- logs, jobs e artifacts.

Portanto, estudar o Codex ajuda principalmente a definir **como agentes utilizam a infraestrutura**, e não quais ferramentas básicas precisam ser instaladas novamente.

## 8. Relação com outros recursos da Fase 2

### LangChain

LangChain é referência para agentes, ferramentas, integrações e workflows. Codex é referência mais específica para o executor de desenvolvimento.

### n8n

n8n é referência para composição de workflows e integrações. Codex é referência para execução interativa de tarefas de programação.

### ClawFlows

ClawFlows mostra workflows reutilizáveis, versionados e agendáveis. Codex mostra um agente especializado trabalhando diretamente em um workspace.

### OFFPack

OFFPack mostra como dependências podem ser pré-cacheadas para execução offline. Isso complementa o problema de disponibilidade de ferramentas/dependências no SandBox.

## 9. O que NÃO fazer

- Não transformar SandBox em clone do Codex.
- Não colocar o Codex como cérebro do IaBrain.
- Não acoplar o IaBrain a uma implementação específica de coding agent.
- Não copiar código sem respeitar Apache-2.0, NOTICE e demais obrigações aplicáveis.
- Não duplicar ferramentas já fornecidas pelo RootFS sem necessidade.
- Não assumir que o fluxo de desktop/terminal do Codex é automaticamente adequado ao Android.

## 10. Perguntas de estudo

1. Como o Codex separa agente, execução e workspace?
2. Como comandos, arquivos e resultados são controlados?
3. Como erros de execução retornam ao agente?
4. Como o agente mantém contexto suficiente sem carregar todo o projeto no modelo?
5. Quais partes podem virar Skills nativas do IaBrain?
6. Quais partes pertencem exclusivamente ao SandBox Runtime?
7. Como adaptar a experiência de coding agent para Android/Termux?
8. Como preservar isolamento, permissões e segurança?
9. Como registrar evidências de build/test para o IaBrain aprender com cada execução?

## 11. Princípio para o projeto

> **Não precisamos adotar o Codex. Precisamos estudar como um coding agent utiliza um ambiente real de desenvolvimento e transformar esse conhecimento em capacidades nativas do ecossistema IaBrain + SandBox.**

A separação permanece:

**IaBrain pensa. Agentes trabalham. SandBox executa.**
