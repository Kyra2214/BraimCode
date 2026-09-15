# BrainCode — Fase 2.5

## Security Lab como sistema adversarial de aprendizado

**Status:** conceito consolidado / não implementar antes do fechamento do Marco 1 e das conexões do núcleo.

**Data:** 2026-09-15

> **Brain constrói. Security tenta quebrar. Evidence comprova. Critic valida. Memory aprende.**

---

## 1. A ideia central

A Fase 2.5 nasce de uma ideia simples e, ao mesmo tempo, extremamente poderosa:

> **Se o Brain constrói e defende, o Security Lab pode tentar quebrar — de forma autorizada, isolada, mensurável e repetível.**

O objetivo não é transformar o Brain em um scanner de segurança nem transformar o Security Lab em mais uma ferramenta interna do Brain.

O objetivo é criar uma fronteira entre dois sistemas especializados:

- **BrainCode:** constrói, corrige, testa e aprende com resultados;
- **Security Test Lab:** ataca, valida hipóteses de vulnerabilidade, mede impacto e aprende a criar testes melhores.

Os dois evoluem de forma independente e se encontram somente através de contratos de capacidade e resultado.

---

## 2. A metáfora da guerra

A metáfora usada para esta fase é uma guerra controlada:

```text
             BRAINCODE
          DEFENSOR / CONSTRUTOR
                  │
                  │ cria / corrige
                  ▼
            SISTEMA/TARGET
                  │
                  │ é submetido a teste
                  ▼
          SECURITY TEST LAB
            ATACANTE CONTROLADO
                  │
                  │ encontra / valida
                  ▼
             SECURITY RESULT
                  │
                  ▼
                BRAIN
                  │
                  │ corrige
                  ▼
            novo ciclo
```

Não é uma guerra real contra terceiros.

É um ciclo de validação contra **projetos e ambientes autorizados**, preferencialmente isolados e reproduzíveis.

A força da ideia está no ciclo:

```text
Construir
   ↓
Atacar
   ↓
Descobrir
   ↓
Validar
   ↓
Corrigir
   ↓
Atacar novamente
   ↓
Verificar se a correção resistiu
   ↓
Aprender
   ↓
Repetir
```

---

## 3. Security como app dentro do app

A integração visual desejada é:

```text
BrainCode
├── Chat
├── Brain
├── Memory
├── Planner
├── Testes do Brain
└── Security Lab
      ├── Scope Guard
      ├── Test Registry
      ├── Adversarial Engine
      ├── Learning Agent
      ├── Security Memory
      └── AI especializada para desenvolvimento de testes
```

O Security Lab pode aparecer como uma **aba/app interno**, mas sua implementação permanece encapsulada.

### Regra fundamental

> **Brain conhece a capacidade Security e recebe seus resultados. Brain não conhece nem controla a implementação dos testes ofensivos.**

Isso evita transformar o núcleo do BrainCode em um emaranhado de lógica específica de segurança.

---

## 4. A fronteira de informação

O Brain **PODE receber**:

- status do teste/campanha;
- PASS/FAIL;
- severidade;
- finding validado;
- impacto medido;
- alvo autorizado;
- evidência sanitizada;
- recomendação de correção;
- cobertura/resumo;
- identificador/proveniência do resultado.

O Brain **NÃO recebe como regra de integração**:

- payloads ofensivos internos;
- sequência detalhada de ataque;
- hipóteses privadas do Adversarial Engine;
- heurísticas internas de exploração;
- lógica interna do Learning Agent;
- implementação dos probes;
- catálogo privado de estratégias ofensivas;
- mecanismos internos usados para gerar novos ataques.

A fronteira existe justamente para permitir que os dois lados evoluam independentemente.

---

## 5. Contrato entre os sistemas

O Brain deve enxergar algo conceitualmente parecido com:

```text
SecurityCapability
    ↓
SecurityResult
```

Um resultado mínimo pode conter:

```text
SecurityResult
- campaignId
- targetId
- status
- validated
- severity
- findingType
- impact
- evidenceReference
- recommendation
- provenance
- timestamp
```

O contrato não deve expor detalhes desnecessários do ataque.

### Estados possíveis

- `PASS`
- `FAIL`
- `PARTIAL`
- `UNCONFIRMED`
- `BLOCKED_BY_SCOPE`
- `INFRA_FAILURE`
- `NOT_APPLICABLE`

O Brain aprende principalmente de resultados **validados**.

---

## 6. O Security Lab continua sendo especialista

O Security Test Lab já possui uma arquitetura própria e não deve ser desmontado para caber dentro do BrainCode.

A especialização permanece:

```text
Scope / Authorization Guard
          ↓
Test Registry
          ↓
Adversarial Engine
          ↓
Recon → Attack → Exploit → Chain → Validate → Prove → Report → Continue
```

O Guard continua sendo a autoridade de escopo.

O Adversarial Engine pode ser agressivo **somente dentro do escopo permitido**.

A integração com BrainCode não muda essa regra.

---

## 7. O segundo cérebro: Learning Agent do Security

Uma das ideias mais importantes da Fase 2.5 é permitir que o próprio Security aprenda a criar testes especializados.

Mas há uma diferença fundamental:

> **A IA pode propor um teste. Ela não ganha autoridade para executá-lo.**

Fluxo:

```text
Security Memory
      ↓
resultados anteriores
      ↓
Learning Agent
      ↓
AI especializada
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
      ↓
novo ciclo
```

Assim, a IA participa da evolução do atacante sem se tornar a autoridade de execução.

---

## 8. NewTestProposal

Uma proposta gerada pelo Learning Agent deve ser estruturada, nunca texto livre executável.

Conceitualmente:

```text
NewTestProposal
- id
- category
- hypothesis
- preconditions
- targetConstraints
- parameters
- validationMethod
- impactMeasurement
- timeout
- requestBudget
- concurrency
- expectedEvidence
- justification
- provenance
```

Antes de entrar no catálogo:

```text
AI proposal
   ↓
Schema validation
   ↓
Security validation
   ↓
Scope validation
   ↓
Authorization validation
   ↓
Registry
```

Qualquer proposta que viole o escopo é descartada.

---

## 9. Evolução dos testes

A ideia não é simplesmente executar 300 testes para sempre.

O catálogo inicial é o ponto de partida.

Exemplo conceitual:

```text
300 testes iniciais
       ↓
seleciona ~15 relevantes
       ↓
executa
       ↓
resultado
       ↓
Learning Agent analisa
       ↓
propõe teste novo
       ↓
validação
       ↓
executa novo teste
       ↓
resultado
       ↓
propõe outro
       ↓
...
```

O Security pode, portanto, evoluir de:

**catálogo estático → catálogo adaptativo → catálogo especializado.**

Mas somente testes aprovados e reproduzíveis podem entrar como conhecimento reutilizável.

---

## 10. Aprendizado operacional, não treinamento de pesos

A Fase 2.5 **não exige treinamento de LLM**.

O aprendizado ocorre operacionalmente.

### Brain aprende

- quais problemas foram encontrados;
- quais correções resolveram;
- quais padrões de implementação falharam;
- quais soluções sobreviveram à validação;
- quais resultados foram confirmados em múltiplos contextos.

### Security aprende

- quais hipóteses foram válidas;
- quais testes encontraram problemas;
- quais combinações foram relevantes;
- quais propostas novas foram validadas;
- quais estratégias não produziram resultado;
- quais padrões merecem novos testes.

Isso pode ser armazenado como conhecimento estruturado, Skills, evidências e procedimentos validados.

---

## 11. O ouro dos 5.000 projetos

Aqui está uma das ideias mais ambiciosas desta fase.

Imagine o sistema operando em **5.000 projetos autorizados**.

Cada projeto gera ciclos de:

```text
Projeto
  ↓
Brain constrói/corrige
  ↓
Security testa
  ↓
Finding ou PASS
  ↓
Validação
  ↓
Conhecimento
```

O valor não está simplesmente em armazenar 5.000 relatórios.

O valor está em extrair padrões reutilizáveis.

### Do lado ofensivo

O Security pode acumular conhecimento sobre:

- classes recorrentes de falhas;
- condições que tornam uma hipótese válida;
- testes que realmente encontraram problemas;
- testes que falharam e por quê;
- combinações relevantes de condições;
- novos testes descobertos e validados.

### Do lado defensivo

O Brain pode acumular conhecimento sobre:

- correções que sobreviveram à validação;
- padrões arquiteturais mais resistentes;
- erros de implementação recorrentes;
- soluções que funcionaram em diferentes projetos;
- tipos de mudança que exigem nova rodada de Security.

Isso cria um ciclo de conhecimento:

```text
5.000 projetos
      ↓
experimentos autorizados
      ↓
resultados
      ↓
validação
      ↓
conhecimento ofensivo + defensivo
      ↓
novas Skills / testes / padrões
      ↓
melhores projetos
      ↓
mais experimentos
```

---

## 12. O conhecimento não pode virar lixo

A escala cria um risco: transformar milhares de resultados em uma coleção de informações contraditórias.

Portanto:

> **Resultado não é verdade. Proposta não é verdade. Finding suspeito não é verdade.**

O caminho deve ser:

```text
Raw Result
   ↓
Evidence
   ↓
Critic / Validator
   ↓
Candidate Knowledge
   ↓
Cross-check / Revalidation
   ↓
Validated Knowledge
   ↓
Reusable Skill / Pattern
```

O conhecimento deve manter:

- proveniência;
- contexto;
- versão;
- evidência;
- nível de confiança;
- histórico de correções;
- número de confirmações;
- limites de aplicabilidade.

Uma descoberta válida em um tipo de sistema não deve automaticamente ser considerada universal.

---

## 13. Memory ofensiva e Memory defensiva

A separação conceitual recomendada é:

```text
                 KNOWLEDGE
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
   Security Memory        Brain Memory
      ofensiva               defensiva
          │                     │
          ↓                     ↓
 ataques/testes          soluções/padrões
          │                     │
          └──────────┬──────────┘
                     ↓
              resultados validados
```

A integração não exige compartilhar todas as memórias.

O Brain recebe os resultados de segurança necessários para tomar decisões.

O Security mantém sua memória especializada de testes e evolução.

---

## 14. A Capability no BrainCode

No BrainCode, Security deve aparecer como uma **capability especializada**.

Conceitualmente:

```text
CapabilityDefinition
    id = security.test
    category = SECURITY
    provider = SecurityTestLab
```

O Brain faz:

```text
Chat
 ↓
Brain
 ↓
Capability Discovery
 ↓
security.test
 ↓
Policy
 ↓
Action Gateway
 ↓
Security Lab
```

O Brain não chama diretamente classes internas do Security.

Isso preserva a arquitetura consolidada:

> **Brain decide. Registry descobre. Policy autoriza. Gateway executa. Sandbox protege. Evidence registra. Critic valida. Memory aprende.**

---

## 15. Segurança do próprio Security

O Security Learning Agent é uma área de alto risco e deve ter mais restrições, não menos.

Regras obrigatórias:

1. nenhuma saída de IA é executada diretamente;
2. toda proposta passa por schema validation;
3. toda proposta passa pelo Scope Guard;
4. toda proposta passa pela autorização do alvo;
5. todo teste possui timeout e request budget;
6. toda execução ocorre no ambiente permitido;
7. evidências sensíveis são mascaradas;
8. não há acesso a credenciais do host;
9. não há acesso ao Docker socket do host;
10. não há escape do ambiente autorizado;
11. resultados são registrados com proveniência;
12. o catálogo evoluído é versionado.

---

## 16. Brain continua tendo seus próprios testes

É importante não confundir os papéis.

### Testes do Brain

Validam:

- código;
- lógica;
- build;
- comportamento funcional;
- integração;
- regressão;
- execução correta de capacidades.

### Testes do Security

Validam:

- resistência contra comportamento adversarial;
- autenticação/autorização;
- entradas maliciosas controladas;
- exposição indevida;
- falhas de configuração;
- cadeias de vulnerabilidade;
- impacto mensurável;
- outras hipóteses de segurança dentro do escopo.

Um lado não substitui o outro.

---

## 17. O ciclo completo da Fase 2.5

```text
                  ┌──────────────────────┐
                  │       BrainCode      │
                  │ construir / corrigir │
                  └──────────┬───────────┘
                             │
                             ▼
                       projeto/target
                             │
                             ▼
                  ┌──────────────────────┐
                  │    Security Lab      │
                  │    Scope Guard       │
                  └──────────┬───────────┘
                             │
                             ▼
                    Test Registry
                             │
                             ▼
                  Adversarial Engine
                             │
                             ▼
                  ataque controlado
                             │
                             ▼
                       Evidence
                             │
                             ▼
                    Security Result
                             │
                             ▼
                         Brain
                             │
                    ┌────────┴────────┐
                    │                 │
                  PASS              FAIL
                    │                 │
                    │                 ▼
                    │             correção
                    │                 │
                    │                 ▼
                    │          novo ciclo
                    │
                    ▼
                  Critic
                    │
                    ▼
                  Memory
```

Paralelamente, dentro do Security:

```text
Security Result
      ↓
Security Memory
      ↓
Learning Agent
      ↓
AI especializada
      ↓
NewTestProposal
      ↓
Validator + Scope Guard
      ↓
Test Registry
      ↓
Adversarial Engine
      ↓
novo resultado
```

Os dois ciclos podem evoluir em paralelo sem fundir suas responsabilidades.

---

## 18. O efeito de escala

Com poucos projetos, a Fase 2.5 já produz valor local.

Com dezenas, começa a aparecer repetição.

Com centenas, começam a aparecer padrões.

Com milhares, pode surgir uma camada de conhecimento generalizável — desde que a proveniência e o contexto sejam preservados.

A visão de longo prazo é:

```text
Projeto A ─┐
Projeto B ─┤
Projeto C ─┤
Projeto D ─┤
...        ├──→ conhecimento validado
Projeto N ─┤
...        ┤
Projeto 5000┘
```

Não é necessário que os projetos compartilhem seus dados privados.

O que pode ser acumulado são **padrões abstratos, resultados sanitizados, evidências permitidas e conhecimento validado**, respeitando as fronteiras de cada projeto.

---

## 19. O que NÃO fazer

A Fase 2.5 não deve:

- colocar o Adversarial Engine dentro do Brain;
- copiar o Security Test Lab inteiro para o BrainCode;
- permitir que o Brain controle os probes internos;
- permitir que o Learning Agent execute diretamente uma saída de IA;
- transformar resultados não validados em verdade;
- misturar memória ofensiva com memória defensiva sem governança;
- permitir testes fora do escopo autorizado;
- usar projetos de terceiros sem autorização;
- substituir os testes normais do Brain pelos testes ofensivos;
- transformar Security em um simples scanner passivo;
- criar outra arquitetura paralela de Registry/Policy/Gateway.

---

## 20. Relação com o Roadmap Canônico

A Fase 2.5 **não substitui** o roadmap atual.

O Marco 0 continua consolidado.

O Marco 1 — fechar o caminho real de execução — continua sendo prioridade antes de adicionar novas camadas arquiteturais.

A Fase 2.5 deve ser tratada como um **marco especializado posterior**, iniciado quando o núcleo estiver suficientemente conectado para expor uma capability Security limpa.

Regra:

> **Primeiro fechar o Brain. Depois conectar o atacante.**

A implementação deve reaproveitar:

- Capability Model;
- Capability Registry;
- Capability Discovery;
- PolicyBroker;
- ActionGateway;
- AgentRegistry;
- SkillRegistry;
- Evidence/Event;
- Critic;
- Memory.

Não criar versões paralelas desses componentes dentro do Security.

---

## 21. Ordem futura de implementação

Quando a Fase 2.5 for iniciada:

### 2.5.1 — Contrato

Criar o contrato `SecurityCapability` / `SecurityResult` entre BrainCode e Security Lab.

### 2.5.2 — Integração como capability

Registrar Security no Capability Registry do BrainCode.

### 2.5.3 — Policy

Garantir que toda chamada Security passe por Policy + ActionGateway.

### 2.5.4 — App-in-app

Criar a aba/tela Security sem expor a implementação interna ao Brain.

### 2.5.5 — Execução

Conectar somente o contrato de entrada e saída ao Security Lab.

### 2.5.6 — Aprendizado ofensivo

Adicionar Security Memory + Learning Agent.

### 2.5.7 — NewTestProposal

Implementar proposta estruturada de novos testes.

### 2.5.8 — Validação

Adicionar pipeline:

`Proposal → Validator → Scope Guard → Registry → Execution`.

### 2.5.9 — Evolução

Permitir que testes novos validados retornem ao catálogo.

### 2.5.10 — Aprendizado cruzado

Somente depois, estudar como transformar resultados sanitizados de múltiplos projetos em padrões reutilizáveis.

---

## 22. Critério de sucesso

A Fase 2.5 estará realmente funcionando quando:

- Brain conseguir solicitar uma validação Security por capability;
- Policy controlar a operação;
- ActionGateway registrar a execução;
- Security receber apenas o escopo necessário;
- Security executar testes dentro do seu próprio ambiente;
- Brain receber apenas o contrato de resultado definido;
- Brain conseguir corrigir o sistema com base no resultado;
- Security conseguir aprender e propor testes novos;
- nenhum teste novo for executado sem validação e autorização;
- resultados validados puderem alimentar memória;
- o ciclo puder ser repetido automaticamente de forma segura;
- os componentes continuarem desacoplados.

---

## 23. Visão final

A ideia mais ambiciosa da Fase 2.5 não é simplesmente adicionar segurança ao BrainCode.

É criar um **ciclo evolutivo entre construção e ataque**.

```text
                 BRAIN
              CONSTRÓI
                  │
                  ▼
              SECURITY
               ATACA
                  │
                  ▼
               EVIDENCE
                  │
                  ▼
                CRITIC
                  │
          ┌───────┴───────┐
          ▼               ▼
        BRAIN           SECURITY
       APRENDE          APRENDE
          │               │
          ▼               ▼
       DEFESA           ATAQUE
          │               │
          └───────┬───────┘
                  ▼
              NOVO CICLO
```

Em pequena escala, isso melhora um projeto.

Em grande escala, com milhares de projetos **autorizados** e conhecimento cuidadosamente validado, pode formar uma biblioteca viva de padrões defensivos e testes ofensivos reproduzíveis.

A grande regra permanece:

> **O atacante não vira o defensor. O defensor não vira o atacante. Eles evoluem separados e conversam por evidências.**

E a frase da fase:

> ## **Brain defende. Security ataca. Critic julga. Memory aprende.**
