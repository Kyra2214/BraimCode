# Fechamento do backlog local offline — Item 5

Data: 2026-09-13

## Escopo fechado

O item 5 do backlog Android offline foi implementado sem servidor, provider externo ou reconstrução dos RootFS homologados.

### 1. Toolchains — rollback transacional e cache

`ToolchainManager` agora mantém uma transação local antes de cada instalação, confirma o estado somente após validação pós-instalação e tenta rollback automático quando a instalação ou validação falha. O estado e o cache de metadados são gravados atomicamente em arquivos locais.

`ToolchainTransactionStore` mantém:

- snapshot do estado anterior;
- pacotes declarados pelo perfil;
- identificador determinístico da transação;
- cache do último status/versionamento observado;
- limpeza do snapshot somente após sucesso confirmado.

A operação `rollback(id)` também pode ser acionada explicitamente. SDK/NDK de implantação continuam fora do escopo offline.

### 2. Security Test Lab — simulação determinística + corpus persistente

`SecurityRegressionCorpus` foi integrado ao `SandboxPlatform`. A suíte baseline usa resultados sintéticos determinísticos para:

- redaction de segredo sintético;
- path traversal;
- command injection;
- SSRF para destino reservado;
- capability bypass;
- tampering de evidência.

A simulação não abre rede, não executa payload adversarial e não depende de alvo externo. Cada execução é registrada no corpus JSONL local e possui digest SHA-256 do corpus.

O `runSecurityRegression()` executa `ProjectScanner → probes determinísticos → SecurityAssessmentEngine → corpus`, preservando o readiness gate já existente.

### 3. BrainExecutionCoordinator

A implementação avançada já existente no módulo `:brain` permanece a API de coordenação de planos de múltiplas etapas, com Policy, aprovação, retry, eventos e memória. O caminho Android já possui execução de planos e retomada via `BrainSandboxController`; a camada de integração local agora mantém os subsistemas Brain expostos de forma programática, sem provider externo.

### 4. DefaultPromptGenerator

`BrainIntegrationFacade` agora expõe geração determinística de prompts de roadmap e prompts de correção usando `DefaultPromptGenerator` e `PromptLibrary` locais.

Não há chamada de rede nem geração por provider externo nesse caminho.

### 5. APIs/Events locais

`BrainIntegrationFacade` agora registra eventos locais de operações de workflow/memória e expõe:

- `localEvents(runId)`;
- `localEventsHealthy()`;
- geração de prompts;
- registro de experiências;
- workflow local.

A integridade continua baseada no `EventStore` existente.

## Testes adicionados

`OfflineBacklogTest` cobre:

- determinismo da suíte de Security Test Lab;
- persistência e digest do corpus;
- round-trip do snapshot/cache de toolchain.

## Limites preservados

Este fechamento não inclui:

- APK Release/keystore/assinatura;
- servidor ou backend distribuído;
- Postgres/Redis/etcd;
- coordenação multi-host;
- isolamento OS-level de produção dependente do host;
- reconstrução ou alteração dos RootFS `0.3.3`, `0.4.1` e `0.5.0` homologados.

O backlog local offline fica encerrado em código/documentação. A validação funcional final em device/emulador continua sendo um gate separado de implantação.
