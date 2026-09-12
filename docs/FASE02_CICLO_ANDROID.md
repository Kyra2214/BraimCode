# Fase 2 — Ciclo Android unificado

`BrainSandboxExecutionBridge` fornece uma entrada única para o ciclo Android. A UI chama o bridge, e o ciclo existente executa Router, Policy, abertura da sessão Sandbox, capacidade e validação do workspace na ordem definida. Assim, não há executor alternativo que possa contornar a autorização.

A compilação foi tentada com Gradle 8.7, mas o ambiente atual não possui Android SDK configurado (`ANDROID_HOME`/`local.properties`). A falha é de ambiente, não de resolução de código; a instalação reproduzível está documentada em `docs/FASE01_AMBIENTE_ANDROID.md`.
