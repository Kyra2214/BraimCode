# BrainCode — Estado Atual

**HEAD de referência:** `26ab43afede8c05c6bcb4e981a0e98f70c42c8a6`.

## Verde / consolidado

- Capability model universal em `:brain`.
- Capability Registry.
- Capability Discovery e candidatos.
- Capability Provider/discovery lazy.
- PolicyBroker.
- ActionGateway e auditoria de ação.
- SkillRegistry.
- Bounded Agents.
- Memória de conhecimento e ciclo de aprendizado.
- Routing/catalogação dinâmica de APIs/providers.
- Integração de capability/policy com o Sandbox.
- Testes JVM relacionados ao núcleo 2.0.
- CI atual com unit tests, APK debug e Android lint.

## Parcial / ainda exige integração ou evidência

- conversa Android usando o gateway real em todos os caminhos relevantes;
- retrieval executor que consuma automaticamente retrieval hints;
- validação semântica forte de conhecimento;
- validação de código por build/test/lint antes de elevar confiança;
- deduplicação e versionamento de conhecimento;
- durable jobs/workflows em todos os caminhos de produto;
- hardening OS-level do Sandbox.

## Backlog arquitetural

1. Retrieval executor.
2. Evidence/citation contract mais estruturado.
3. Knowledge deduplication/fingerprint.
4. Knowledge versioning/corrections.
5. Critic semântico com validação executável.
6. Planner/ExecutionPlan em todos os fluxos complexos.
7. Dispatcher/Workflow/DAG com lifecycle completo.
8. Durable JobStore onde houver tarefas longas.
9. Auto-Skills somente após validação forte.
10. Hardening OS-level, trust chain e limites de recursos.

## Fora de escopo atual

- baixar LLM local;
- treinar modelo;
- criar Agent com LLM próprio;
- copiar runtime do IaBrain;
- importar Room/schema do IaBrain;
- criar catálogos enormes apenas para substituir os atuais;
- adicionar componentes sem caller real.

## Regra de conclusão

Uma funcionalidade só é considerada pronta quando houver, conforme aplicável:

1. implementação;
2. teste;
3. caller real;
4. evidência observável;
5. documentação atualizada.

A existência de uma classe isolada ou de um teste unitário isolado não prova integração do produto.
