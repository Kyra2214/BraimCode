# Auditoria dos últimos 30 commits — BrainCode

**Data:** 2026-09-14  
**HEAD auditado:** `4a94538fd92d83a82974df9bc10749a9a1b3b34f`  
**Janela:** do commit `f6fd073bc19bda577df362e30ed82b8a91a3bb93` até o HEAD.  
**Escopo:** execução local, LLM, catálogo de APIs, roteamento, fallback, persistência de conhecimento, proveniência, Critic, Android e documentação.

## 1. Método

Foram revisados os 30 commits mais recentes do repositório e o diff agregado entre o primeiro commit da janela e o HEAD. A comparação contém 29 commits posteriores ao commit-base; contando o próprio commit-base, a janela auditada contém 30 commits.

O critério usado foi:

> implementação real + comportamento observável + caller real quando aplicável + testes existentes + documentação coerente.

A auditoria não transforma código presente em funcionalidade integrada automaticamente. Onde a integração Android ainda não foi comprovada, o estado permanece **parcial**.

## 2. Resumo dos 30 commits

### Local LLM / Sandbox

- `f6fd073` — evita baixar novamente o engine llama já instalado; o fast path reutiliza o engine persistido no RootFS.

### Catálogo e APIs gratuitas

- `048826b` — amplia catálogo de providers gratuitos.
- `fca3dd2` — documenta inicialmente free/paid no catálogo de chaves.
- `b1948f2` — restringe catálogo a APIs estritamente gratuitas.
- `f8bd384` — expõe somente providers gratuitos.
- `b83a899` — roteia somente modelos estritamente gratuitos.
- `857f717` — adiciona waterfall automático entre providers gratuitos.
- `6109d06` — documenta o waterfall gratuito.
- `fc50fb4` — descoberta dinâmica dos modelos atuais por API.
- `31d3c0b` — refresh do catálogo gratuito sob demanda.
- `886b34c` — testes da descoberta dinâmica e filtro de preço.
- `b916cbf` — registry do catálogo em runtime.
- `9662d98` — catálogo dinâmico ligado ao router.
- `d2c97f7` — controller ligado ao catálogo em runtime.
- `dde94f7` — remove DeepSeek pago do catálogo free-only.
- `29422e0` — mantém somente famílias de providers verificadas como free-tier.
- `046909f` — usa endpoints de descoberta específicos por provider.
- `943bccc` — atualiza o provider/model selecionado antes do routing.
- `7adac07` — remove documentação obsoleta sobre catálogo estático.

### Execução real do gateway

- `f285c12` — liga execução real do Brain às APIs gratuitas.
- `3f23b55` — corrige o gateway real de APIs gratuitas.
- `7b0f000` — classifica respostas HTTP para fallback inteligente.
- `a87017d` — usa HTTP compatível com Android no gateway.

### Memória, conhecimento e proveniência

- `1f8f49a` — adiciona memória de conhecimento e proveniência.
- `ab34593` — persiste conhecimento aprendido no Android.
- `5681dbe` — instala memória persistente do Brain.
- `ecdcdd2` — fecha o ciclo de aprendizado com proveniência.
- `4c90914` — liga memória, proveniência e reaprendizado ao gateway.

### Critic

- `2f18950` — adiciona gate local do Critic.
- `4a94538` — liga o Critic automaticamente ao ciclo de aprendizado.

## 3. Estado técnico encontrado

### 3.1 Catálogo de APIs

O catálogo deixou de depender de uma lista fixa de modelos como fonte de verdade. O Brain pode descobrir modelos atuais por endpoint do provider, filtrar modelos ativos/chat-capable e aplicar a política de gratuidade.

A política atual é **free-only**. Providers ou modelos pagos/promocionais não devem entrar no caminho automático gratuito.

O catálogo também possui refresh em runtime. Se um modelo selecionado desaparecer, o gateway tenta substituir por um modelo atual compatível com o mesmo papel.

### 3.2 Waterfall e fallback

O caminho de execução possui candidatos alternativos e classificação de falhas HTTP. Exemplos tratados incluem chave inválida, limite, timeout e erros de servidor.

Isso permite que o Brain tente outra opção sem exigir que o usuário escolha manualmente o provider.

### 3.3 Gateway Android

`BrainApiGateway` é o núcleo Android para execução de providers gratuitos. Ele usa `HttpURLConnection`, consulta chave pelo `ApiKeyStore`, passa pelo `ProviderDispatcher` e registra tentativas.

**Limite auditado:** a existência do gateway não prova que a tela de chat Android já esteja roteada por ele. A integração com `SandboxViewModel.sendChatMessage()` continua devendo ser comprovada antes de ser marcada como produto integrado.

### 3.4 Memória de conhecimento

Foi criado um contrato de memória independente do armazenamento, com:

- problema;
- resposta;
- fonte/proveniência;
- repository/path/commit quando encontrados;
- retrieval hints;
- tags;
- confiança;
- estado validado;
- confirmações.

No Android existe persistência local via `AndroidKnowledgeMemory`.

O recall automático consulta somente conhecimento validado.

### 3.5 Aprendizado e correção

O fluxo atual é:

```text
problema
  ↓
memória validada
  ├─ encontrada → reutiliza
  └─ não encontrada
       ↓
     Router
       ↓
   API gratuita
       ↓
 resposta + fonte
       ↓
 candidato de conhecimento
       ↓
     Critic
       ├─ ACCEPT → conhecimento validado
       ├─ UNCERTAIN → permanece candidato
       └─ REJECT → não entra no recall
```

O Brain **não treina nem altera pesos do LLM externo**. Ele registra e corrige conhecimento próprio.

### 3.6 Proveniência GitHub

Quando a resposta contém URL GitHub, o Brain tenta registrar:

- repository;
- path;
- commit/tree/blob quando identificável;
- URI da fonte.

Isso cria a base para o próximo estágio: armazenar não somente a resposta, mas também **como encontrar novamente a solução**.

Ainda falta um executor de retrieval que use automaticamente essas pistas para pesquisar/validar a fonte antes de recorrer a um LLM.

### 3.7 Critic atual

O Critic presente é deliberadamente conservador e determinístico. Ele rejeita resposta vazia, mantém como incerto candidato sem fonte verificável e aceita com confiança moderada resposta não vazia que tenha fonte registrada.

Isso **não é validação semântica completa**. Para código, o próximo nível deve usar Sandbox + build/test/lint; para fatos externos, fonte/cross-check; para respostas determinísticas, validação local.

### 3.8 Local LLM

O engine local instalado no RootFS não deve ser baixado novamente quando a versão já estiver instalada. O fast path verifica o engine e o marker de versão antes de baixar o arquivo novamente.

## 4. Arquitetura resultante

```text
                    BrainCode
                       │
                ┌──────┴──────┐
                │             │
             Brain          Sandbox
                │             │
       ┌────────┼────────┐    │
       │        │        │    │
    Memory    Router   Critic │
       │        │        │    │
       │     APIs/free   │    │
       │        │        │    │
       └────────┴────────┘    │
              │               │
       conhecimento           │
       + proveniência         │
              │               │
              └──── validação ┘
```

A intenção é que o Brain seja a camada de decisão/orquestração e o Sandbox seja a camada de execução controlada.

## 5. Pendências reais após a auditoria

### P0 — Segurança do Sandbox

- isolamento de rede OS-level;
- jail filesystem OS-level;
- isolamento da árvore de processos;
- enforcement OS-level de CPU/memória/PIDs/FDs/disco;
- trust chain autenticada dos RootFS/manifests.

### P1 — Integração

- provar por teste arquitetural `Planner → ExecutionPlan → Policy → AuthorizedPlan → Agent → Sandbox` sem reconstrução a partir do Task;
- bind completo de credentials;
- SSRF/DNS rebinding;
- rotação de EventStore mantendo hash-chain;
- fencing de leases;
- testes de wiring/orphan;
- ligar efetivamente o chat Android ao `BrainApiGateway` antes de declarar essa integração pronta.

### P1 — Conhecimento

- retrieval executor baseado em `retrievalHints`;
- deduplicação/fingerprint;
- versionamento e histórico de correções;
- escopos global/usuário/projeto para futura versão servidor;
- índice de busca escalável;
- Critic semântico via Sandbox/segunda fonte;
- contrato estruturado de citações em vez de depender somente de URLs encontradas no texto.

### P2 — Validação

A última documentação anterior registra uma matriz de testes aprovada em 2026-09-13. Os commits desta janela alteraram código depois daquela validação; portanto **não é correto carregar automaticamente aquele PASS para o HEAD de 2026-09-14**.

É necessário executar novamente testes `:brain`, `:android-module`, `:app`, build Debug e validação funcional do caminho Android antes de afirmar novo PASS.

## 6. Decisões de documentação

1. Catálogo dinâmico é a fonte operacional; seed estático é apenas bootstrap.
2. Free-only permanece a política automática atual.
3. Conhecimento externo começa como candidato.
4. Somente conhecimento validado entra no recall automático.
5. Fonte de documentação do provider não é tratada como evidência factual da resposta.
6. O Critic atual não deve ser descrito como verificador semântico completo.
7. Gateway existente não é sinônimo de chat Android integrado.
8. RootFS homologado não deve ser reconstruído por causa dessas mudanças.
9. Nenhuma funcionalidade deve ser marcada como concluída somente por existir ou passar em teste isolado.

## 7. Resultado

Os últimos 30 commits representam uma mudança arquitetural relevante: o Brain saiu de um catálogo estático de APIs e avançou para **descoberta dinâmica + waterfall gratuito + execução real + memória persistente + proveniência + Critic**.

A próxima fronteira é transformar esse conhecimento armazenado em capacidade de recuperação independente e conectar o fluxo de conversa Android ao gateway sem mascarar a diferença entre biblioteca implementada e produto realmente acionado.
