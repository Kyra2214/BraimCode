# Harness de validação Android ARM64

## Objetivo

Este documento implementa o módulo **Fase 0.1 — Harness ARM64** do plano de atualização de segurança. O harness impede que um build em host x86_64 ou emulador x86 seja apresentado como evidência de validação ARM64. Ele instala o APK, coleta propriedades do dispositivo e executa o smoke test Android existente.

## Pré-requisitos

É necessário um único dispositivo físico ou emulador Android conectado por `adb`, com ABI `arm64-v8a` ou `aarch64`, depuração USB habilitada e o sistema completamente inicializado. O APK deve estar compilado em `app/build/outputs/apk/debug/app-debug.apk`, ou ser informado por `APK=/caminho/app.apk`.

O harness não considera compilação, teste JVM ou execução no host como substitutos para o gate ARM64. Também recusa explicitamente targets não ARM64.

## Execução

A partir da raiz do repositório:

```bash
export ANDROID_HOME=/caminho/para/android-sdk
bash scripts/arm64-harness.sh
```

Para usar um APK específico:

```bash
APK=app/build/outputs/apk/debug/app-debug.apk \
EVIDENCE_DIR=artifacts/arm64-harness-$(date +%Y%m%d-%H%M%S) \
bash scripts/arm64-harness.sh
```

O comando exige exatamente um dispositivo online. Em caso de sucesso, `artifacts/arm64-harness/` conterá as propriedades do dispositivo, `getprop`, `uname`, saída da instalação, saída do smoke test e informações do pacote instalado.

## Critérios de aprovação

O módulo passa somente quando o script confirmar todos os pontos a seguir:

| Critério | Evidência |
|---|---|
| Há exatamente um target `adb` online | Contagem no harness e `device.properties` |
| A ABI é ARM64 | `abi=arm64-v8a` ou `abi=aarch64` |
| O APK foi instalado no target | `install.txt` e `package.txt` |
| O fluxo Android de preparação, execução, diagnóstico e reset passou | `e2e-smoke.txt` contém `E2E smoke passed` |
| As propriedades do target são preservadas | `getprop.txt` e `device-uname.txt` |

## Estado inicial desta implementação

O harness e o procedimento estão versionados. No ambiente de desenvolvimento usado para esta alteração não havia `adb` nem dispositivo/emulador conectado; portanto, a execução ARM64 real permanece pendente e não é declarada como aprovada. A primeira execução em um dispositivo ARM64 deve preservar o diretório de evidências e anexá-lo ao registro de validação do release.

Este módulo não implementa os attack probes da Fase 0.2, o gate arquitetural da Fase 0.3 nem qualquer correção da Fase 1.
