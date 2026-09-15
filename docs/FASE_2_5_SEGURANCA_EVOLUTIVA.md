# BrainCode — Fase 2.5: Segurança Evolutiva

**Status:** conceito arquitetural consolidado — não implementar automaticamente antes da consolidação da Fase 2.

> **Brain constrói. Security tenta quebrar. Evidence comprova. Critic valida. Memory aprende.**

## 1. A ideia central

A Fase 2.5 nasce de uma ideia simples e ambiciosa: o BrainCode não deve apenas testar se o código funciona. Ele deve poder colocá-lo diante de um adversário controlado.

O **Security Test Lab** continua sendo um projeto especializado e separado. No BrainCode ele aparece como uma capacidade/aplicativo interno, com uma fronteira clara entre os dois sistemas.

O Brain conhece a capacidade de segurança e recebe seus resultados. Ele **não conhece nem controla a implementação interna dos testes ofensivos**.

Isso cria dois lados independentes:

- **BrainCode:** constrói, corrige e defende.
- **Security Test Lab:** tenta encontrar maneiras autorizadas de quebrar.

O objetivo não é transformar o Brain em um scanner de segurança. O objetivo é criar um ciclo de confronto controlado que produza conhecimento validado.

---

## 2. Por que Fase 2.5

Ela não deve substituir as fases principais do BrainCode. É uma camada que entra depois que a infraestrutura de capacidades, políticas, execução, memória e evidências estiver consolidada.

A Fase 2.5 também evita um erro arquitetural: copiar o Security Test Lab para dentro do BrainCode. Isso criaria duplicação e misturaria responsabilidades.

A integração correta é por contrato:

```text
BrainCode
   │
   │ capability: security.test
   ↓
Security Test Lab
   │
   ├── Scope / Authorization Guard
   ├── Test Registry
   ├── Adversarial Engine
   ├── Learning Agent
   ├── Security Memory
   └── specialized security APIs
   │
   ↓
SecurityResult
   │
   ↓
BrainCode
```

---

## 3. A fronteira de segurança

### Brain pode saber

- que uma avaliação de segurança foi executada;
- qual projeto/alvo autorizado foi avaliado;
- status geral;
- severidade consolidada;
- quantidade de achados;
- se uma vulnerabilidade foi validada;
- impacto medido de forma resumida;
- evidências permitidas pelo contrato;
- recomendações consolidadas;
- cobertura geral.

### Brain não deve receber

- payloads ofensivos internos;
- sequência completa dos ataques;
- hipóteses privadas do adversarial engine;
- detalhes internos de exploração;
- lógica de geração de testes;
- prompts internos usados para criar testes;
- implementação dos probes;
- memória operacional privada do Security;
- mecanismos internos que permitiriam ao Brain reproduzir/controlar o ataque.

A fronteira é deliberada.

> **Brain consome resultado de segurança, não o mecanismo ofensivo.**

---

## 4. Security como aplicativo dentro do aplicativo

Na interface do BrainCode, Security pode aparecer como uma aba ou módulo próprio, funcionando conceitualmente como um aplicativo dentro do aplicativo.

Isso permite:

- isolamento de responsabilidade;
- interface própria;
- memória de segurança separada;
- ciclo ofensivo independente;
- evolução independente;
- contrato de saída controlado;
- possibilidade de trocar ou evoluir o Security Test Lab sem reescrever o Brain.

Não significa necessariamente que todo o código do Security deva ser incorporado ao APK. A fronteira arquitetural é mais importante que a forma física de empacotamento.

---

## 5. Dois sistemas de aprendizado

A ideia mais importante da Fase 2.5 é que existem **dois aprendizados diferentes**.

### 5.1 Aprendizado defensivo — BrainCode

O Brain recebe resultados validados de segurança e pode aprender:

- quais classes de problema foram encontradas;
- quais correções resolveram os problemas;
- quais padrões de implementação foram frágeis;
- quais soluções resistiram a novas avaliações;
- quais práticas devem ser reutilizadas.

Isso alimenta a memória e o conhecimento defensivo do Brain.

### 5.2 Aprendizado ofensivo — Security

O Security aprende internamente:

- quais hipóteses de ataque foram úteis;
- quais testes produziram resultados relevantes;
- quais combinações de condições merecem investigação;
- quais testes foram inúteis ou redundantes;
- quais novos testes podem aumentar a cobertura.

Esse aprendizado permanece dentro do Security.

O Security não treina pesos de uma LLM. Trata-se de **aprendizado operacional**: resultados, hipóteses, evidências, testes validados e padrões reutilizáveis.

---

## 6. O ciclo de guerra controlada

A metáfora de "guerra" é útil para explicar o conceito, mas o ambiente deve permanecer estritamente autorizado e controlado.

```text
                 ┌───────────────┐
                 │   BrainCode   │
                 │    DEFESA     │
                 └───────┬───────┘
                         │
                    código/projeto
                         │
                         ↓
              ┌────────────────────┐
              │   Security Lab     │
              │      ATAQUE        │
              └─────────┬──────────┘
                        │
                  resultado
                        ↓
              ┌────────────────────┐
              │      Evidence      │
              └─────────┬──────────┘
                        ↓
                  ┌───────────┐
                  │  Critic   │
                  └─────┬─────┘
                        ↓
                     Memory
                        │
                        ↓
                  correção/reteste
                        │
                        └──────────────→ Security
```

O ciclo pode repetir até atingir os critérios definidos para o projeto.

---

## 7. O Security não é o teste normal do Brain

Essa distinção é obrigatória.

### Testes do BrainCode

Verificam principalmente:

- compilação;
- comportamento funcional;
- testes unitários;
- integração;
- E2E;
- regressões;
- contratos;
- qualidade interna.

### Testes do Security Test Lab

Tentam encontrar falhas de segurança dentro do escopo autorizado:

- autenticação/autorização;
- exposição indevida;
- entrada malformada;
- configuração insegura;
- falhas de isolamento;
- vulnerabilidades de aplicação;
- combinações de condições que aumentem impacto;
- outras categorias existentes no catálogo do Security.

Um código pode passar por todos os testes normais e ainda falhar no Security.

Portanto:

> **Teste funcional pergunta: "funciona?"**
>
> **Security pergunta: "consigo quebrá-lo dentro do escopo autorizado?"**

---

## 8. Security Learning Agent

O Security poderá possuir um agente próprio, mas ele será **amarrado ao domínio de segurança**.

Ele não recebe autoridade para executar qualquer coisa gerada por IA.

Fluxo:

```text
Security Memory
      ↓
resultados anteriores
      ↓
Learning Agent
      ↓
especialized security API
      ↓
NewTestProposal
      ↓
Test Validator
      ↓
Scope / Authorization Guard
      ↓
Test Registry
      ↓
Adversarial Engine
      ↓
resultado
      ↓
Security Memory
```

A IA serve para **desenvolver hipóteses/testes especializados**. Ela não é a autoridade de execução.

---

## 9. NewTestProposal

Um novo teste gerado pelo Security Learning Agent deve ser uma estrutura declarativa e validável, não código arbitrário executável diretamente.

Campos conceituais:

```text
id
category
hypothesis
preconditions
allowedTarget
parameters
validationMethod
impactMeasurement
timeout
requestBudget
concurrency
evidenceExpected
justification
provenance
```

Antes de entrar no catálogo, o teste deve passar por validação estrutural e pelo Scope/Authorization Guard.

Somente depois pode ser executado.

---

## 10. Evolução dos testes

Exemplo conceitual:

1. Security possui centenas de testes conhecidos.
2. Para determinado projeto, seleciona os testes relevantes.
3. Executa os testes dentro do escopo autorizado.
4. Gera um relatório.
5. O Learning Agent analisa os resultados.
6. Uma API especializada sugere uma nova hipótese.
7. Security transforma a sugestão em `NewTestProposal`.
8. Validator verifica o contrato.
9. Scope Guard verifica autorização e limites.
10. O novo teste é registrado.
11. O teste é executado.
12. O resultado volta para a memória do Security.
13. O ciclo pode gerar outra hipótese.

O importante é que **cada geração passa novamente pelas mesmas barreiras**.

Não existe:

```text
LLM → execução direta
```

Existe:

```text
LLM → proposta → validação → autorização → registro → execução
```

---

## 11. O catálogo de 5.000 projetos

Aqui está uma das ideias mais ambiciosas da fase.

Imagine milhares de projetos autorizados passando pelo mesmo ciclo.

Não significa compartilhar código privado ou dados sensíveis entre projetos. O valor está em extrair **padrões generalizáveis e validados**, com proveniência e contexto.

Exemplo:

```text
Projeto 37
   ↓
novo padrão de teste validado
   ↓
Security Knowledge
   ↓
Projeto 814
   ↓
hipótese relevante
   ↓
novo resultado
```

Outro exemplo:

```text
Projeto 1.200
   ↓
correção defensiva validada
   ↓
Knowledge Compiler
   ↓
Skill defensiva reutilizável
   ↓
Projeto 4.700
```

Assim, cada projeto pode contribuir para a evolução do conhecimento **sem transformar a memória em um depósito indiscriminado de relatórios**.

---

## 12. O verdadeiro ativo: conhecimento validado

O objetivo não é acumular 5.000 relatórios.

O objetivo é transformar experiências em conhecimento reutilizável:

```text
Raw Result
    ↓
Evidence
    ↓
Candidate Knowledge
    ↓
Critic
    ↓
Validated Knowledge
    ↓
Reusable Pattern / Skill
```

Cada conhecimento deve manter:

- origem/proveniência;
- contexto;
- evidência;
- confiança;
- validações/confirmacões;
- data;
- escopo de aplicabilidade;
- relação com testes ou correções quando permitido.

---

## 13. Segurança aprende ataque; Brain aprende defesa

Essa separação cria um efeito interessante.

### Security acumula

**"Como encontrar problemas."**

### Brain acumula

**"Como evitar/corrigir problemas."**

E o resultado pode formar um ciclo:

```text
Security encontra padrão
        ↓
Brain corrige
        ↓
Security retesta
        ↓
resultado validado
        ↓
Brain aprende defesa
Security aprende ataque
```

Quanto maior a quantidade de projetos autorizados, maior a oportunidade de encontrar padrões recorrentes.

Mas a regra permanece:

> **Generalização somente após validação e com proveniência.**

---

## 14. Critic como juiz

O Critic não pertence exclusivamente a um dos lados conceitualmente.

Ele deve validar o que pode ser promovido para conhecimento reutilizável.

Possíveis resultados:

- `PASS`
- `FAIL`
- `CORRECT`
- `RETRY`
- `UNCONFIRMED`
- `OUT_OF_SCOPE`
- `INFRA_FAILURE`

Um achado suspeito não vira automaticamente verdade.

Uma sugestão de teste também não vira automaticamente um teste oficial.

---

## 15. Política e isolamento

A Fase 2.5 herda as regras do Security Test Lab.

O ambiente ofensivo deve permanecer:

- autorizado;
- delimitado por escopo;
- isolado;
- sem acesso a credenciais do host;
- sem acesso arbitrário ao ambiente externo;
- com orçamento de requisições;
- com timeout;
- com concorrência controlada;
- com evidências mascaradas quando necessário;
- com limpeza restrita aos recursos do próprio laboratório.

O Brain não pode usar a integração de Security para contornar essas regras.

---

## 16. Capability do BrainCode

A integração deve aparecer para o Brain como uma capacidade, por exemplo:

```text
security.test
```

O Brain descobre essa capacidade pelo `CapabilityRegistry`/`CapabilityDiscovery`.

O Brain pode solicitar uma avaliação com parâmetros declarativos, por exemplo:

```text
capability = security.test
project = X
scope = autorizado
mode = validation
```

O Action Gateway/Policy Broker decide se a ação pode ocorrer.

O Security executa internamente seu próprio fluxo.

O Brain recebe somente o contrato de resultado.

---

## 17. Contrato SecurityResult

Modelo conceitual:

```text
SecurityResult
├── assessmentId
├── targetId
├── status
├── validated
├── severitySummary
├── findingsCount
├── impactSummary
├── coverageSummary
├── recommendations
├── evidenceReferences
└── provenance
```

O contrato deve ser pequeno o suficiente para preservar a fronteira entre Brain e Security.

Detalhes ofensivos ficam do lado do Security.

---

## 18. O que NÃO fazer

A Fase 2.5 não deve:

- copiar o Security Test Lab inteiro para BrainCode;
- transformar o Brain em scanner de segurança;
- permitir que uma LLM execute diretamente um teste novo;
- permitir que uma sugestão de IA ignore o Scope Guard;
- compartilhar memória ofensiva privada com o Brain;
- compartilhar dados sensíveis entre projetos;
- transformar resultados não validados em verdade;
- misturar testes funcionais e ofensivos como se fossem a mesma coisa;
- criar outro sistema paralelo de Capability Registry;
- criar outro Agent Registry dentro do Brain se o existente já atender à função;
- criar uma segunda memória genérica sem necessidade.

---

## 19. Fase 2.5 e o BrainCode 2.0

A Fase 2.5 depende da arquitetura que está sendo consolidada:

```text
Chat
  ↓
Brain
  ↓
Memory
  ↓
Capability Discovery
  ↓
Policy Broker
  ↓
Action Gateway
  ↓
Security Capability
  ↓
Security Test Lab
  ↓
SecurityResult
  ↓
Evidence / Critic
  ↓
Memory
```

O Security não substitui nenhuma dessas peças.

Ele é um **especialista externo/encapsulado** conectado pela arquitetura universal de capacidades.

---

## 20. Roadmap conceitual da Fase 2.5

### 2.5-A — Contrato

Definir:

- `security.test`;
- `SecurityResult`;
- limites de entrada/saída;
- proveniência;
- política de autorização.

### 2.5-B — App-in-App

Criar a superfície de Security no BrainCode sem misturar sua implementação interna ao Brain.

### 2.5-C — Execução integrada

Conectar a capability ao Security Test Lab por um gateway controlado.

### 2.5-D — Resultado para o Brain

Entregar somente o contrato de resultado.

### 2.5-E — Aprendizado defensivo

Permitir que resultados validados alimentem o Knowledge/Memory do Brain.

### 2.5-F — Learning Agent ofensivo

No Security, permitir geração de `NewTestProposal` por IA especializada.

### 2.5-G — Evolução controlada

Validator + Scope Guard + Registry + execução + memória em ciclo.

### 2.5-H — Escala

Com múltiplos projetos autorizados, extrair padrões generalizáveis, sempre preservando proveniência, contexto e isolamento.

---

## 21. Critérios de conclusão

A Fase 2.5 só pode ser considerada funcional quando:

- Security estiver acessível como capability real;
- Brain receber apenas `SecurityResult`;
- testes normais e testes ofensivos permanecerem independentes;
- Scope/Authorization Guard estiver efetivamente ativo;
- novas propostas geradas por IA passarem por validação antes da execução;
- resultados tiverem evidência/proveniência;
- Critic impedir promoção automática de conhecimento não validado;
- memória ofensiva permanecer isolada;
- memória defensiva receber somente o conhecimento permitido;
- o ciclo correção → ataque → validação puder ser demonstrado em ambiente autorizado;
- testes automatizados cobrirem os contratos críticos;
- CI comprovar a integração.

---

## 22. Visão final

A ideia não é construir simplesmente um aplicativo que escreve código.

A visão é um sistema onde a criação e a segurança evoluem em conjunto, mas permanecem independentes:

```text
                    BRAINCODE
                 ┌──────────────┐
                 │    Brain     │
                 │   DEFENDE    │
                 └──────┬───────┘
                        │
                 SecurityResult
                        │
                        ↓
              ┌──────────────────┐
              │      CRITIC      │
              │      JULGA       │
              └────────┬─────────┘
                       │
                       ↓
                    MEMORY
                       ↑
                       │
              conhecimento validado
                       │
              ┌────────┴─────────┐
              │  SECURITY LAB    │
              │     ATACA        │
              ├──────────────────┤
              │ Test Registry    │
              │ Adversarial Eng. │
              │ Learning Agent   │
              │ Security Memory  │
              └──────────────────┘
```

### Slogan da Fase 2.5

> **Brain constrói. Security tenta quebrar. Evidence comprova. Critic valida. Memory aprende.**

E a ideia maluca por trás de tudo:

> **Se milhares de projetos autorizados entrarem nesse ciclo, o valor não estará nos milhares de relatórios. Estará nos padrões de ataque e defesa que sobreviveram à validação.**

Isso é o que transforma a Fase 2.5 de uma simples integração de segurança em uma camada de **evolução contínua do BrainCode**.
