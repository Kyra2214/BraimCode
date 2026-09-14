# BrainCode — runtime Brain + Sandbox Mobile

BrainCode é um runtime local-first que combina **Policy, approval, eventos auditáveis, Sandbox, workflows, memória, routing, skills, QA, descoberta de APIs e readiness** com um cliente Android offline baseado em RootFS/proot.

> **Estado real em 2026-09-14:** os últimos 30 commits adicionaram descoberta dinâmica de modelos, política free-only, waterfall/fallback entre APIs, gateway Android de providers, memória persistente de conhecimento, proveniência e Critic automático. A integração vertical Android → Brain → Policy → Capability → Sandbox continua comprovada para `sandbox.health`; o novo `BrainApiGateway` existe e executa APIs, mas a integração da conversa Android com esse gateway ainda não deve ser tratada como comprovada até existir caller real no fluxo de chat. Consulte `docs/AUDITORIA_30_COMMITS_2026-09-14.md`.

## Arquitetura

```text
BrainCode
├── brain_runtime/       # runtime Python de referência
├── brain/               # Brain Kotlin/JVM
├── android-module/      # bridge, policy/capabilities e Sandbox
├── app/                 # cliente Android Compose
├── contracts/           # contratos e invariantes
├── tests/               # testes Python
└── docs/                # auditorias, roadmap e documentação operacional
```

Fronteira conceitual:

```text
Brain pensa / roteia / autoriza
        ↓
Agent / Capability
        ↓
Sandbox executa sob limites
        ↓
Evidence / Events / Delivery
```

Para conhecimento externo, o fluxo atual é:

```text
Problema
  ↓
Memória validada
  ├─ encontrou → reutiliza
  └─ não encontrou
       ↓
   Router / API gratuita
       ↓
 resposta + proveniência
       ↓
 candidato de conhecimento
       ↓
      Critic
       ↓
 conhecimento validado
```

O Brain não treina pesos de LLM externo. Ele registra, valida e corrige conhecimento próprio.

## APIs e modelos

O catálogo operacional é dinâmico. O Brain consulta endpoints de descoberta dos providers, filtra modelos ativos/chat-capable e aplica a política atual **free-only**.

O catálogo estático/seed serve apenas como bootstrap. O runtime pode atualizar o provider antes do routing e substituir um modelo que tenha desaparecido por outro modelo atual compatível com o papel solicitado.

O fallback é automático e classifica falhas como chave inválida, limite, timeout e erro de servidor quando aplicável. O usuário não precisa escolher manualmente o provider.

## Execução Android de APIs

`BrainApiGateway` é a camada Android para execução interna dos providers gratuitos. Ele usa `ApiCatalogRegistry`, `DefaultAIRouter`, `ProviderDispatcher`, `ApiKeyStore` e transporte HTTP compatível com Android.

**Limite importante:** o gateway está implementado, mas a existência dele não significa que a tela de conversa já esteja roteada por ele. Essa integração só será marcada como concluída após comprovação de caller real no fluxo Android.

## Memória e aprendizado

A memória de conhecimento registra:

- problema e resposta;
- fonte/URI;
- provider/model;
- repository/path/commit quando identificáveis;
- retrieval hints;
- tags;
- confiança;
- estado de validação;
- confirmações/correções.

No Android, o conhecimento é persistido localmente por `AndroidKnowledgeMemory`.

Respostas externas entram primeiro como **candidatas**. O `ConservativeKnowledgeCritic` pode confirmar automaticamente somente quando há resposta não vazia e fonte verificável; candidatos sem evidência permanecem fora do recall automático.

O Critic atual é estrutural/evidencial, não uma prova semântica completa. Código deverá futuramente passar por Sandbox/build/test/lint ou segunda fonte antes de receber confiança mais alta.

## Proveniência e recuperação futura

Quando uma resposta contém GitHub, o Brain tenta guardar repository, path, commit/tree/blob e URI. Isso é a base para o próximo estágio: o Brain aprender **onde e como procurar** uma solução, e não apenas armazenar a resposta.

Ainda falta o executor que use automaticamente esses `retrievalHints` para procurar e validar a fonte antes de consultar uma API/LLM novamente.

## Local LLM

O engine local é instalado no RootFS e reutilizado enquanto a versão instalada corresponde ao marker esperado. Depois da primeira instalação, o BrainCode não baixa novamente o arquivo do engine apenas para iniciar outra conversa.

## Integração Android real

O caminho vertical comprovado no código é:

```text
SandboxViewModel.runBrainHealthCheck()
  → BrainSandboxController.healthCheck()
  → BrainSandboxExecutionBridge
  → CicloExecucaoPlano
  → PolicyBroker
  → AgentSandboxSession
  → CapabilityResolver
  → ManagedSandboxRuntime / proot
```

A UI também possui operações reais para Plugins, Workspace, Git status, Services/SQLite, TestLab, Security assessment e Toolchains.

O workflow da aba Operações continua local/demonstrativo e não representa uma execução real no Sandbox. Caminhos que não passam por Policy/Capability não devem ser usados como prova de segurança do núcleo.

## Python runtime

`brain_runtime/` continua sendo a implementação de referência com PolicyBroker, approval, pipeline, binding, sandbox, workflows, APIs, skills, memory/learning, observabilidade, Project Intelligence, readiness e release intelligence.

## Validação

Os documentos anteriores registram uma matriz aprovada em 2026-09-13. Como houve código novo depois dela, essa aprovação não é automaticamente válida para o HEAD de 2026-09-14.

Para uma nova validação completa:

```bash
./gradlew test
./gradlew check
./gradlew :app:assembleDebug
python3 -m unittest discover -s tests -p 'test_*.py' -v
bash scripts/validate-release-readiness.sh
bash -n scripts/*.sh rootfs-builder/*.sh
```

Não declarar novo BUILD/TEST PASS até essa matriz ser executada novamente no HEAD atual.

## Segurança

O projeto possui hardening significativo, mas não deve ser tratado como container ou isolamento OS-level de produção. Proot sozinho não fornece jail de filesystem, namespace de rede, isolamento completo de processos ou enforcement completo de recursos do host.

O Security Test Lab usa análise estática e regressão sintética/determinística segura. Isso não equivale a um ataque adversarial real contra o Sandbox.

## RootFS

Os RootFS homologados do antigo SandBox foram migrados sem rebuild:

- `0.3.3`
- `0.4.1`
- `0.5.0`

Tamanhos, SHA-256, sidecars e manifests foram preservados. Não reconstruir esses artefatos sem nova homologação explícita.

## Documentação principal

- **Auditoria dos últimos 30 commits:** `docs/AUDITORIA_30_COMMITS_2026-09-14.md`
- **Auditoria técnica:** `AUDITORIA_PESADA.md`
- **Mapa de integração:** `docs/MAPA_INTEGRACAO_2026-09-14.md`
- **Apresentação:** `docs/APRESENTACAO_BRAINCODE.md`
- **Plano de ação:** `PLANO_DE_ACAO.md`
- **Roadmap:** `ROADMAP_UNIFICADO.md`
- **Pendências:** `TAREFAS_PENDENTES.md`
- **Security Test Lab:** `docs/SECURITY_TEST_LAB.md`
- **Migração RootFS:** `docs/SANDBOX_RELEASE_MIGRATION.md`
- **Contratos:** `contracts/`

## Critério de conclusão

Uma funcionalidade só deve ser marcada como concluída quando houver:

1. implementação;
2. teste automatizado;
3. chamada real fora do próprio teste, a partir da UI, ViewModel ou runtime ensinado pelo README;
4. evidência observável;
5. documentação coerente com o comportamento observado.

Esse critério impede que código existente ou testado isoladamente seja apresentado como integração pronta.
