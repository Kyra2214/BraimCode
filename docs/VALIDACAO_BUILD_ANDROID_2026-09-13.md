# Validação Android e APK — 2026-09-13

## Ambiente

A validação foi executada com JDK 17 e Android SDK local configurado em `/home/ubuntu/android-sdk`. O projeto foi compilado com o Gradle Wrapper.

## Testes direcionados

Os testes direcionados do módulo `app` foram executados com:

```bash
./gradlew :app:testDebugUnitTest --no-daemon \
  --tests 'com.sandbox.sandbox.AuthorizedCapabilityExecutorTest' \
  --tests 'com.sandbox.sandbox.NetworkPolicyTest' \
  --tests 'com.sandbox.sandbox.RemotePluginCatalogTest' \
  --tests 'com.sandbox.sandbox.SearchablePluginManagerTest' \
  --tests 'com.sandbox.sandbox.ToolchainModelsTest'
```

Resultado: **PASS** — build concluído com sucesso.

## Suíte completa

```bash
./gradlew test --no-daemon
```

Resultado: **PASS** — `BUILD SUCCESSFUL`, 97 tarefas acionáveis.

## APK debug

```bash
./gradlew :app:assembleDebug --no-daemon
```

Resultado: **PASS** — `BUILD SUCCESSFUL`.

Artefato:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Tamanho: `17,760,522` bytes.

SHA-256:

```text
1915da8a81843fe966f97f8816e7d0161d401cdc2e6e9a3e4dae671166c1917b
```

O APK é uma build **debug**. A validação em dispositivo/emulador ARM64 não foi executada nesta sessão.
