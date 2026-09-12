# Entrega das cinco fases — 2026-09-12

## Escopo executado

Esta entrega percorre as cinco frentes pendentes identificadas no `ROADMAP_UNIFICADO.md`. O objetivo foi transformar cada frente em uma base executável, testável ou explicitamente preparada para integração, sem declarar como concluídas capacidades que dependem de Android SDK, JDK 17, dispositivo físico ou infraestrutura OS-level.

## Fase 1 — Brain e LLM local

Foi criado `LocalLLMSecretario`, um adapter do `Secretario` para qualquer provider local compatível com `ProviderClient`, incluindo servidores locais de llama.cpp, Ollama ou Qwen. A classe exige resposta JSON estruturada, limita o texto enviado, rejeita respostas inválidas e usa `KeywordSecretario` como fallback determinístico. O fallback impede que indisponibilidade ou baixa confiança do modelo produza uma classificação inventada.

Testes adicionados cobrem resposta válida, indisponibilidade do provider e JSON inválido.

## Fase 2 — Toolchain Android

O catálogo declarativo agora inclui o perfil `android`, com detecção por `sdkmanager` e pacotes allowlisted para SDK, platform-tools e build-tools. O perfil não instala nada automaticamente: continua sujeito a Policy, autorização, readiness, licenças e disponibilidade do host. SDK/NDK, cache, rollback transacional e licenças ainda exigem validação no ambiente Android de implantação.

## Fase 3 — Security Test Lab

Foi criado `SecurityScenarioCatalog` com seis cenários baseline bloqueantes: redaction de secrets sintéticos, path traversal, command injection, SSRF para destinos reservados, bypass de capability e tampering de evidência. O catálogo valida duplicidade e impede que cenários das fronteiras críticas sejam declarados permissivos por engano. A execução permanece delegada ao Sandbox autorizado; não há payload contra terceiros nem abertura de rede implícita.

## Fase 4 — Android e release

A base de código e o roadmap foram mantidos honestos quanto ao limite de implantação: a validação de `:app` depende de Android SDK e a toolchain Gradle exige JDK 17. Permanecem como checklist de release a compilação em JDK 17, teste em emulador/dispositivo, RootFS/proot real, assinatura do APK e validação de lifecycle.

## Fase 5 — Infraestrutura de produção

A implementação local continua deny-by-default e não simula cgroups, Bubblewrap, firewall, namespaces ou coordenação distribuída. Para produção, o operador deve fornecer essas garantias e configurar Postgres/Redis/etcd quando houver necessidade de leases e workflows multi-host. A ausência desses serviços deve bloquear readiness em modo protegido, não ser convertida em falso sucesso.

## Validação desta entrega

| Verificação | Resultado |
|---|---|
| `python3 -m unittest discover -s tests -q` | **134 testes aprovados** |
| `./gradlew :brain:test` | Bloqueado: o projeto exige JDK 17 e o ambiente possui JDK 21 |
| Android `:app` | Bloqueado até `ANDROID_HOME`/SDK configurado |
| Working tree antes da entrega | Limpo, branch `main` alinhada ao remoto |
| Segurança | Sem secrets reais, rede externa ou execução adversarial implícita |

## Pendências que continuam abertas

A entrega não substitui a validação de produção. Continuam abertas a integração de transporte real do LLM local, download/licenciamento de SDK/NDK, executor OS-level de probes, corpus persistente de regressão, cgroups/Bubblewrap/firewall, backend distribuído, assinatura de release e validação em dispositivo físico.

## Critério para chamar as cinco fases de prontas

Cada bloqueio externo deve ser convertido em evidência de implantação: build com JDK 17, testes Android, execução do RootFS em dispositivo/emulador, relatório do Security Test Lab com todas as probes baseline, readiness sem blockers, assinatura verificável e teste de recovery com backend escolhido. Até lá, o status correto é **base implementada, validação de implantação pendente**.
