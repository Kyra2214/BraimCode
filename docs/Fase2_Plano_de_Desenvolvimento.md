# Fase 2 — Plano de Desenvolvimento

## Visão geral

A Fase 2 define o IaBrain como um sistema de inteligência/orquestração com autoaprendizado, pesquisa na Internet, descoberta de conhecimento e recursos reutilizáveis. O IaBrain não é apenas um LLM com ferramentas: ele coordena LLM, memória, Skills, Prompts, APIs, conhecimento, agentes, conectores, revisão e execução.

Princípio central:

> **IaBrain pensa. Agentes trabalham. SandBox executa.**

## 1. Autoaprendizado

O IaBrain aprende com experiências anteriores:

- sucesso e falha;
- qualidade do resultado;
- erros e correções;
- custo e tempo;
- agentes utilizados;
- Skills utilizadas;
- Prompts utilizados;
- APIs utilizadas;
- conectores utilizados;
- ferramentas utilizadas;
- fontes consultadas;
- estratégias adotadas.

A rede de aprendizado deve aprender as relações entre esses elementos, e não apenas avaliar cada elemento isoladamente.

Exemplo:

`Problema → Fonte → Estratégia → API → Conector → Skill → Prompt → Agente → Resultado → Avaliação`

Com o tempo, o IaBrain pode descobrir quais combinações apresentam melhores resultados para cada classe de problema.

## 2. Pesquisa na Internet

Quando o conhecimento interno ou o LLM local não forem suficientes, o IaBrain poderá pesquisar na Internet antes de recorrer a uma IA externa mais cara.

A pesquisa deve:

- buscar documentação;
- consultar fontes oficiais;
- consultar GitHub e código real;
- consultar fóruns e comunidades quando necessário;
- comparar fontes;
- identificar limitações e problemas conhecidos;
- registrar a origem das informações;
- atribuir confiança às descobertas.

Regra: **priorizar fontes confiáveis e informar de onde a informação foi obtida.**

A pesquisa não serve somente para responder perguntas. Ela também serve para descobrir como executar um projeto e preparar o caminho para APIs, Conectores, Skills, Prompts e agentes.

## 3. Banco / Catálogo de APIs

O IaBrain terá um catálogo de APIs e recursos disponíveis.

Cada recurso poderá registrar:

- nome;
- capacidade;
- função;
- entradas;
- saídas;
- requisitos;
- custo;
- disponibilidade;
- limitações;
- confiabilidade;
- histórico de resultados;
- situações em que funciona melhor.

O IaBrain pesquisa, descobre e avalia APIs antes de escolhê-las para uma tarefa.

### 3.1 Catálogos externos como fontes de descoberta

Como referência inicial para estudo e descoberta, o IaBrain poderá analisar catálogos públicos de APIs e recursos, incluindo o trabalho de **Chris Porter (`cporter202`)**, especialmente:

- `social-media-scraping-apis` — catálogo de APIs/serviços para coleta de dados de redes sociais;
- `API-mega-list` — catálogo amplo de APIs organizado por categorias;
- outros repositórios do autor relacionados a APIs e recursos para agentes.

Esses repositórios devem ser tratados como **fontes de estudo e descoberta**, não como dependências obrigatórias do IaBrain. O sistema deverá extrair metadados, capacidades, categorias e referências úteis e manter seu próprio catálogo interno.

O objetivo é estudar como grandes catálogos podem ser indexados e transformados em uma camada de descoberta capaz de responder perguntas como:

`Objetivo → capacidade necessária → recursos candidatos → custo/limitações → escolha → execução → resultado → avaliação`

A incorporação de qualquer código ou conteúdo externo deve respeitar a licença correspondente e manter os créditos e avisos exigidos.

### 3.2 Repositórios adicionais de estudo — Agentic AI

Durante a Fase 2 também serão estudados repositórios do ecossistema **cporter202** que vão além de simples listas de APIs. Eles poderão servir como material de pesquisa para projetar o catálogo, a descoberta de capacidades, o planejamento e a composição de agentes do IaBrain.

#### `agentic-ai-apis`

Catálogo focado especificamente em infraestrutura para sistemas agentic, organizado em:

- Agents;
- AI Models;
- MCP Servers.

É especialmente interessante para estudar como separar **execução/autonomia**, **modelos de inteligência** e **integrações/ferramentas MCP** em uma camada de descoberta. O conteúdo é dinâmico e atualizado automaticamente, portanto os números e entradas devem ser tratados como dados que mudam com o tempo.

O repositório informa que seus links mantêm rastreamento de afiliados e que os dados são sincronizados do catálogo da Apify. Portanto, será estudado como **fonte de descoberta**, e não como fonte neutra ou verdade absoluta.

#### `agentic-ai-starters`

Biblioteca de blueprints práticos para transformar capacidades em sistemas completos. Cada starter pode conter:

- conceito do produto;
- fluxo de trabalho;
- arquitetura;
- Prompts;
- stack de APIs;
- caminhos de implementação;
- exemplos de agentes e operações;
- em alguns casos, ideias de produto/monetização.

É particularmente relevante para estudar a relação:

`Problema → Workflow → Arquitetura → APIs → Prompts → Agentes → Execução`

Também será usado para estudar padrões de **research agents**, **lead generation**, **SEO/content**, **social listening**, **MCP toolchains** e **multi-agent operations**.

#### `ai-agent-tools`

Será estudado como uma fonte mais ampla de ferramentas, utilitários e recursos relacionados a agentes e desenvolvimento de IA. O objetivo é identificar capacidades que possam futuramente entrar no catálogo de ferramentas/Skills do IaBrain.

#### Outros catálogos relacionados

Também poderão ser analisados, conforme a necessidade da Fase 2:

- `openclaw-api-list`;
- `coreclaw-api-directory`;
- `scraping-apis-for-devs`;
- `social-media-scraping-apis`;
- `video-scraping-apis`;
- outros catálogos especializados encontrados durante a pesquisa.

A finalidade não é copiar esses projetos para dentro do IaBrain. É estudar suas estruturas, categorias, metadados, relações entre capacidades e provedores e transformar o conhecimento útil em um **catálogo interno próprio, pesquisável e avaliável**.

### 3.3 Confiança e qualidade das fontes externas

Catálogos externos podem possuir vieses comerciais, links afiliados, dados duplicados, entradas desatualizadas ou critérios próprios de seleção.

Por isso, a Fase 2 deverá estudar um mecanismo de avaliação de fontes que registre, quando possível:

- origem da informação;
- data da descoberta;
- provedor real;
- documentação oficial;
- licença;
- custo;
- dependências;
- evidências de funcionamento;
- confiabilidade histórica;
- resultados obtidos pelo próprio IaBrain;
- existência de rastreamento/afiliados;
- conflitos ou limitações conhecidas.

O princípio será:

`Fonte externa → descoberta → validação → catalogação → teste → confiança → uso`

Nunca:

`Fonte externa → confiança cega → execução`

## 4. Banco de Prompts

O IaBrain terá uma biblioteca de Prompts operacionais.

Cada Prompt poderá possuir:

- versão;
- finalidade;
- contexto de uso;
- Skill relacionada;
- agente relacionado;
- resultado obtido;
- taxa de sucesso;
- custo;
- tempo;
- histórico de melhorias.

O sistema aprende quais Prompts funcionam melhor para cada situação e pode melhorar ou substituir Prompts com base nos resultados.

## 5. Banco de Conhecimento / Descobrimento

Além do catálogo de APIs, o IaBrain terá um banco de conhecimento descoberto.

Exemplo de uma pesquisa sobre IPTV:

`documentação → GitHub → fóruns → protocolos → bibliotecas → APIs → exemplos → problemas conhecidos`

Essas descobertas podem posteriormente alimentar:

**Prompt + Skill + API + Conector + Agent + Roadmap + Estratégia**.

O sistema também aprende quais fontes são mais confiáveis para cada tipo de assunto.

## 6. Skills

O IaBrain utilizará Skills reutilizáveis como unidades de capacidade e conhecimento operacional.

Como fonte inicial, poderá utilizar o repositório de Skills de **Matt Pocock (`mattpocock/skills`)**, respeitando integralmente sua licença, avisos de copyright e créditos aplicáveis.

As Skills externas poderão ser:

- importadas;
- catalogadas;
- adaptadas;
- testadas;
- avaliadas;
- combinadas com outras Skills;
- relacionadas a Prompts, APIs, Conectores e agentes;
- utilizadas como dados para o aprendizado do IaBrain.

O objetivo não é simplesmente copiar uma coleção de Skills, mas incorporá-las ao ecossistema de capacidades do IaBrain e permitir que o sistema descubra quais Skills funcionam melhor em cada cenário.

## 7. Galeria / Catálogo de Conectores

O IaBrain deverá possuir uma camada própria de **Conectores**, responsável por descobrir, catalogar, instalar/configurar quando apropriado e disponibilizar integrações com serviços externos.

O sistema poderá utilizar galerias e registros públicos como fontes de descoberta. Entre as referências iniciais estão o **MCP Registry**, o **Docker MCP Registry** e o **GitHub MCP Registry**.

Esses registros devem ser tratados como fontes de conectores, e não como o cérebro do IaBrain. O IaBrain mantém seu próprio catálogo interno e decide qual conector utilizar.

Um conector representa uma capacidade de integração, não um agente. Exemplos:

- GitHub;
- Google Drive;
- Gmail;
- Google Calendar;
- YouTube;
- Discord;
- Telegram;
- bancos de dados;
- serviços HTTP/API;
- outros serviços que possam ser integrados de forma segura.

Cada conector poderá registrar:

- identificador e nome;
- serviço/provedor;
- capacidades expostas;
- operações disponíveis;
- entradas e saídas;
- protocolo/interface (por exemplo MCP ou API própria);
- requisitos de autenticação;
- permissões necessárias;
- dependências;
- custo;
- disponibilidade;
- compatibilidade;
- versão;
- origem/registro;
- licença;
- confiabilidade;
- histórico de falhas e sucessos;
- Skills relacionadas;
- APIs relacionadas;
- agentes que o utilizam;
- tarefas em que apresentou bons resultados.

O IaBrain deve conseguir escolher entre diferentes conectores para a mesma capacidade e aprender, através dos resultados, qual opção apresenta melhor custo, tempo, confiabilidade e qualidade.

Exemplo:

`Objetivo → capacidades necessárias → catálogo de conectores → autenticação/permissões → conector → Skill → agente → SandBox → resultado → avaliação → aprendizado`

A arquitetura deve permitir adicionar ou substituir um conector sem alterar o cérebro do IaBrain.

## 8. Agentes especialistas

O IaBrain poderá trabalhar com agentes especializados, por exemplo:

- Android;
- Backend;
- Frontend;
- Banco de dados;
- APIs;
- Segurança;
- QA/Testes;
- Documentação;
- Player/Multimídia;
- EPG/Cast e outros especialistas conforme a necessidade.

O IaBrain decide dinamicamente quais agentes são necessários para cada problema.

## 9. LLM interno

O LLM interno é um **secretário/analista linguístico**, não o cérebro inteiro.

Ele ajuda a:

- interpretar solicitações;
- estruturar requisitos;
- resumir informações;
- organizar conhecimento;
- sugerir planos;
- criar/adaptar Prompts;
- analisar resultados e erros.

O IaBrain continua responsável por estado, memória, decisões, permissões, recursos, agentes, execução, avaliação e aprendizado.

O LLM pode ser substituído sem reconstruir o cérebro do sistema.

## 10. Planejamento e Roadmap

Fluxo básico:

`Problema → Pesquisa → Descoberta → Roadmap → Divisão por funcionalidades → Agentes`

Um projeto grande pode ser dividido em várias funcionalidades independentes, permitindo que diferentes agentes trabalhem em paralelo quando apropriado.

## 11. Revisão e QA

Um agente afirmar que terminou não significa que o trabalho está concluído.

O IaBrain deve:

1. executar testes;
2. analisar resultados;
3. solicitar revisão independente quando necessário;
4. rejeitar resultados incorretos;
5. escolher outra estratégia em caso de falha;
6. repetir a execução até atingir os critérios de sucesso.

## 12. Recuperação de falhas

Quando um agente ficar travado, o IaBrain pode:

- alterar o Prompt;
- trocar a Skill;
- pesquisar documentação;
- encontrar outra API;
- trocar o Conector;
- chamar outro agente;
- consultar outra IA;
- solicitar revisão;
- alterar a estratégia;
- tentar novamente.

A falha deixa de ser apenas um erro e passa a ser também informação para o aprendizado.

## 13. Memória de experiências

A memória registra a experiência completa de execução:

`problema → estratégia → Skill → Prompt → API → conector → agente → ferramentas → fontes → erros → resultado → testes → custo → tempo → avaliação → correção`

Essa memória alimenta diretamente o mecanismo de autoaprendizado.

## 14. Rede de autoaprendizado

A rede não deve ser apenas uma representação visual de agentes conectados.

Ela representa relações aprendidas entre:

- problemas;
- estratégias;
- agentes;
- Skills;
- Prompts;
- APIs;
- conectores;
- ferramentas;
- fontes;
- resultados;
- revisores;
- custo;
- tempo;
- qualidade;
- erros.

Inicialmente, o aprendizado pode utilizar pesos, pontuações, confiança, frequência, recompensa, penalização e histórico. Posteriormente, poderá evoluir para modelos de aprendizado mais sofisticados.

## 15. Fluxo completo

```text
                    IA BRAIN
                       │
        ┌──────────────┼──────────────────┐
        ↓              ↓                  ↓
     MEMÓRIA         LLM             AUTOAPRENDIZADO
        │              │                  │
        └──────────────┼──────────────────┘
                       ↓
                  DESCOBRIMENTO
                       │
        ┌──────────────┼──────────────────┐
        ↓              ↓                  ↓
   INTERNET        APIs              CONECTORES
        │              │                  │
        └──────────────┼──────────────────┘
                       ↓
                PROMPT DATABASE
                       ↓
                 SKILL CATALOG
                       ↓
              CONNECTOR CATALOG
                       ↓
                AGENT CATALOG
                       ↓
                  ESTRATÉGIA
                       ↓
              AGENTES ESPECIALISTAS
                       ↓
                    SANDBOX
                       ↓
                 TESTE / QA
                       ↓
                  RESULTADO
                       ↓
              MEMÓRIA + APRENDIZADO
                       ↺
```

## 16. Hierarquia de utilização de recursos

Como princípio econômico, o IaBrain pode priorizar recursos mais baratos antes de recursos externos mais caros:

```text
Memória
↓
Conhecimento existente
↓
LLM local
↓
Ferramentas
↓
Conectores disponíveis
↓
Pesquisa na Internet
↓
APIs especializadas
↓
Outras IAs
```

Essa hierarquia não é rígida. O próprio aprendizado poderá descobrir quando outro caminho é melhor.

## 17. SandBox

O SandBox permanece como ambiente independente de execução.

Ele fornece o corpo operacional para:

- terminal;
- arquivos;
- Git;
- compiladores;
- SDKs;
- testes;
- processos;
- execução de código;
- bibliotecas;
- serviços;
- logs;
- artefatos.

O SandBox não precisa saber se quem está solicitando uma execução é o IaBrain, outro agente ou um usuário.

> **IaBrain decide. Agentes trabalham. SandBox executa.**

## 18. Objetivo da Fase 2

Construir a base para que o IaBrain deixe de ser apenas um orquestrador de tarefas e passe a ser um sistema adaptativo capaz de:

- descobrir conhecimento;
- descobrir recursos;
- encontrar APIs;
- encontrar Conectores;
- encontrar e avaliar Skills;
- encontrar e melhorar Prompts;
- selecionar agentes;
- criar estratégias;
- executar no SandBox;
- testar e revisar;
- detectar falhas;
- mudar de estratégia;
- registrar experiências;
- aprender quais caminhos funcionam melhor;
- reutilizar esse aprendizado em problemas futuros.

### Princípio final

> **IaBrain não é uma IA que sabe fazer tudo. É um sistema que sabe o que sabe, reconhece o que não sabe, sabe onde procurar, sabe qual recurso utilizar, sabe qual inteligência chamar, sabe quando trocar de estratégia, sabe quando pedir ajuda, sabe quando revisar e aprende com cada experiência.**
