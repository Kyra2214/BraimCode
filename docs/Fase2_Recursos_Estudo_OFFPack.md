# Fase 2 — Recurso de Estudo: OFFPack

## Referência

Projeto: `Assemou007/OFFPack`

Descrição: gerenciador leve de dependências npm offline, escrito em Go e sem dependências externas, criado para baixar pacotes e dependências transitivas uma vez e depois permitir instalações sem Internet.

## Por que estudar no SandBox / IaBrain

O OFFPack é interessante para a Fase 2 porque trabalha diretamente com uma necessidade que combina com a filosofia do SandBox: **execução local, cache, ambientes isolados e redução de dependência de rede**.

A ideia não é incorporar o OFFPack automaticamente ao SandBox, mas estudar sua arquitetura e avaliar posteriormente se algumas ideias devem virar uma capacidade própria do ambiente.

## Pontos para estudar

- cache local de pacotes npm;
- armazenamento por pacote e versão;
- cache de dependências transitivas;
- instalação totalmente offline;
- resolução semver sem biblioteca externa;
- metadados/manifestos locais;
- pré-cache de pacotes populares;
- atualização controlada do cache;
- instalação de pacotes scoped;
- integração com `package.json`;
- ferramenta CLI pequena escrita em Go;
- funcionamento sem dependências externas;
- estratégia para ambientes air-gapped e CI restrito.

## Relação com o SandBox

Possíveis ideias a avaliar futuramente:

`Projeto → dependências → pré-cache → execução offline → resultado`

Isso pode ser especialmente útil para:

- builds repetitivos;
- projetos Android/Node dentro do RootFS;
- CI com conectividade limitada;
- ambientes de agentes que precisam repetir builds sem baixar tudo novamente;
- recuperação rápida após reinstalação/reset do ambiente;
- preparação de um ambiente de trabalho para agentes.

## Relação com o IaBrain

O IaBrain poderá futuramente catalogar recursos de cache como uma capacidade do SandBox:

`Necessidade → dependência → cache disponível → instalação offline → execução`

Isso também pode entrar na rede de aprendizado da Fase 2, registrando:

- quais dependências foram usadas;
- quais já estavam disponíveis;
- tempo economizado pelo cache;
- falhas de instalação;
- tamanho do cache;
- frequência de reutilização;
- projetos que compartilham dependências.

## O que NÃO fazer agora

- não adicionar OFFPack ao RootFS automaticamente;
- não substituir npm por OFFPack sem testes;
- não copiar código sem revisar a licença;
- não assumir que o cache resolve todos os casos de compatibilidade;
- não transformar uma referência externa em dependência estrutural do SandBox.

Primeiro: **estudar → comparar → testar → medir → decidir**.

## Licença e origem

O README do projeto declara licença MIT. Qualquer reutilização de código deve manter os avisos e créditos exigidos pela licença.

## Perguntas para a Fase 2

1. O cache do OFFPack pode inspirar um `sandbox-package-cache` próprio?
2. Vale manter caches persistentes de npm/pnpm/yarn no SandBox?
3. Podemos compartilhar caches entre projetos sem contaminar ambientes?
4. Como medir economia de tempo, rede e armazenamento?
5. Como integrar o cache com jobs e recuperação de ambientes?
6. O mesmo conceito pode ser aplicado a outras toolchains do RootFS?

### Princípio de estudo

> **Não precisamos adotar o projeto. Precisamos aprender o que ele faz bem e transformar a ideia útil em uma capacidade própria do SandBox.**
