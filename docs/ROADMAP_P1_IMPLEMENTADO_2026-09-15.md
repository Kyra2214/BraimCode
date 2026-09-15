# Roadmap P1/P2 — atualização de 2026-09-15

## Entregas aplicadas

O caminho principal do chat Android agora injeta um `FileEventStore` em `BrainSandboxController`, registrando eventos de criação de tarefa, plano, falha e entrega/validação. O fluxo continua passando pelo planner, autorização da Policy, dispatcher/Agent e Sandbox; os eventos sobrevivem ao reinício do aplicativo em `filesDir/brain/chat-events.jsonl`.

A memória de conhecimento passou a ser local-first e persistente no gateway do chat, em `filesDir/brain/knowledge.jsonl`. O contrato agora suporta citações estruturadas, fingerprint SHA-256, deduplicação, escopos global/usuário/projeto e histórico de correções. O caminho Android existente de `AndroidKnowledgeMemory` foi compatibilizado com esses invariantes.

As telas de API Keys e Plugins passaram a usar rolagem horizontal nas linhas de ações, evitando corte de botões em telas estreitas. O teste de chave continua executando uma chamada real mínima contra o modelo configurado e exibindo sucesso, latência ou erro observável.

## Evidências adicionadas

`KnowledgeMemoryRoadmapTest` cobre deduplicação por fingerprint, preservação de citação, isolamento de escopo e histórico de correção. Os testes existentes de planner, policy, dispatcher, workflow, retrieval, EventStore e Android permanecem parte da suíte Gradle.

## Validação

A validação local foi iniciada com `:brain:test :android-module:test`, mas o sandbox não possui um toolchain Java 17 local configurado para o Gradle e não autoriza download automático. Portanto, a confirmação final de compilação, testes Android, lint e APK será feita pelo CI do GitHub após o push.

A segurança do Sandbox não foi alterada neste trabalho, conforme o escopo explícito do roadmap.
