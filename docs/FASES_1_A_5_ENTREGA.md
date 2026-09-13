# Entrega das cinco fases — 2026-09-12

## Escopo executado

Esta entrega percorre as cinco frentes pendentes identificadas no `ROADMAP_UNIFICADO.md`. O objetivo foi transformar cada frente em uma base executável, testável ou explicitamente preparada para integração, sem declarar como concluídas capacidades que dependem de dispositivo físico ou infraestrutura OS-level.

## Fase 1 — Brain e LLM local

Foi criado `LocalLLMSecretario`, um adapter do `Secretario` para qualquer provider local compatível com `ProviderClient`, incluindo servidores locais de llama.cpp, Ollama ou Qwen. A classe exige resposta JSON estruturada, limita o texto enviado, rejeita respostas inválidas e usa `KeywordSecretario` como fallback determinístico. O fallback impede que indisponibilidade ou baixa confiança do modelo produza uma classificação inventada.

Testes adicionados cobrem resposta válida, indisponibilidade do provider e JSON inválido.

## Fase 2 — Toolchain Android

O catálogo declarativo agora inclui o perfil `android`, com detecção por `sdkmanager` e pacotes allowlisted para SDK, platform-tools e build-tools. O perfil não instala nada automaticamente: continua sujeito a Policy, autorização, readiness, licenças e disponibilidade do host. SDK/NDK, cache, rollback transacional e licenças foram validados no host de build; o comportamento em device continua dependente da implantação Android.

## Fase 3 — Security Test Lab

Foi criado `SecurityScenarioCatalog` com seis cenários baseline bloqueantes: redaction de secrets sintéticos, path traversal, command injection, SSRF para destinos reservados, bypass de capability e tampering de evidência. O catálogo valida duplicidade e impede que cenários das fronteiras críticas sejam declarados permissivos por engano. A execução permanece delegada ao Sandbox autorizado; não há payload contra terceiros nem abertura de rede implícita.

## Fase 4 — Android e release

A base de código e o roadmap foram mantidos honestos quanto ao limite de implantação: a compilação e os testes Gradle foram validados com Android SDK e JDK 17. Permanecem como checklist de release o teste em emulador/dispositivo, RootFS/proot real, assinatura do APK e validação de lifecycle.

## Fase 5 — Infraestrutura de produção

A implementação local continua deny-by-default. cgroups, Bubblewrap, firewall, namespaces, coordenação distribuída e Postgres/Redis/etcd pertencem a uma futura implantação de servidor/host e não fazem parte do escopo atual Android offline.

## Validação desta entrega

| Verificação | Resultado |
|---|---|
| `python3 -m unittest discover -s tests -q` | **134 testes aprovados** |
| `./gradlew :brain:test --no-daemon --rerun-tasks` | **Aprovado** |
| `./gradlew :android-module:test :app:test :app:assembleDebug --no-daemon --rerun-tasks` | **Aprovado — BUILD SUCCESSFUL** |
| Android SDK/JDK | **JDK 17 e SDK 34 configurados e usados** |
| APK Debug | **Gerado**, SHA-256 `e51adf29e818ec073dc89cea247ae6c50f1652a6673995cb6a21ac355bb3b93e` |
| Working tree antes da entrega | Limpo, branch `main` alinhada ao remoto |
| Segurança | Sem secrets reais, rede externa ou execução adversarial implícita |

## Pendências que continuam abertas

A entrega não substitui a validação de device. Continuam abertas a integração de transporte real do LLM local, executor OS-level de probes, corpus persistente de regressão e validação em dispositivo físico. Assinatura de release, backend distribuído e infraestrutura de servidor estão documentados como futuro.

## Critério para chamar as cinco fases de prontas

Cada bloqueio externo deve ser convertido em evidência de implantação: execução do RootFS em dispositivo/emulador, relatório do Security Test Lab com todas as probes baseline, readiness sem blockers, assinatura verificável e teste de recovery com backend escolhido. Build JDK 17, testes Android e empacotamento Debug já possuem evidência no host de build. O status correto é **base implementada, validação de implantação física e release pendente**.


## Validação Android adicional — 2026-09-12

O ambiente foi preparado com **JDK 17** e **Android SDK API 34** em `/home/ubuntu/Android/Sdk`, incluindo Build Tools 34.0.0, platform-tools e NDK 26.3. A validação no clone limpo mais recente também corrigiu o helper de `riskClass` do teste de plano e declarou diretamente `implementation(project(":brain"))` no app.

A validação final passou:

```text
./gradlew :brain:test --no-daemon --rerun-tasks                         BUILD SUCCESSFUL
./gradlew :android-module:test :app:test :app:assembleDebug --no-daemon --rerun-tasks  BUILD SUCCESSFUL
APK: app/build/outputs/apk/debug/app-debug.apk
SHA-256: e51adf29e818ec073dc89cea247ae6c50f1652a6673995cb6a21ac355bb3b93e
Tamanho: 17 MiB
```

O APK é um artefato debug gerado localmente. Ainda não houve instalação em emulador ou dispositivo físico, portanto a validação de runtime Android, RootFS/proot, lifecycle e desempenho continua pendente.
