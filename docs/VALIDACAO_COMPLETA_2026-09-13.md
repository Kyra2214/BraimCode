# Validação completa do BrainCode — 2026-09-13

## Resultado executivo

A validação final foi executada após a instalação das toolchains necessárias e após a correção das falhas encontradas na primeira rodada. O resultado foi aprovado em todas as etapas automatizadas: Python, Kotlin/JVM, Android unit tests, Gradle agregado, lint, montagem do APK debug, preflight dos releases RootFS e sintaxe dos scripts. Um build limpo executado com `--warning-mode=all` também terminou sem warnings de compilador, lint, manifesto ou empacotamento.

A validação continua limitada ao host de build. A instalação em emulador ou dispositivo, a execução real do proot em ARM64, a assinatura de release e a validação de lifecycle continuam sendo gates de implantação separados.

## Ambiente preparado

| Componente | Versão/localização |
|---|---|
| JDK | OpenJDK 17.0.20 em `/usr/lib/jvm/java-17-openjdk-amd64` |
| Gradle | Wrapper Gradle 8.7 |
| Android SDK | API 34 em `/home/ubuntu/Android/Sdk` |
| Android Build Tools | 34.0.0 |
| Android platform-tools | 37.0.1 |
| Android NDK | 26.3.11579264 |
| Python | 3.12.3 |

O procedimento foi consolidado em `scripts/setup-test-dependencies.sh`. A configuração `local.properties` é local e ignorada pelo Git.

## Matriz de validação final

| Área | Comando | Resultado | Evidência |
|---|---|---:|---|
| Python | `python3 -m unittest discover -s tests -p 'test_*.py' -v` | **PASS** | 155 testes aprovados |
| Brain Kotlin/JVM | `./gradlew :brain:test --no-daemon` | **PASS** | 64 execuções entre variantes debug/release |
| Android module | `./gradlew :android-module:test --no-daemon` | **PASS** | 68 execuções entre variantes debug/release |
| App Android | `./gradlew :app:test --no-daemon` | **PASS** | 120 execuções entre variantes debug/release |
| Gradle agregado | `./gradlew test --no-daemon` | **PASS** | `BUILD SUCCESSFUL` |
| Verificação Gradle e lint | `./gradlew check --no-daemon` | **PASS** | `BUILD SUCCESSFUL` |
| APK debug | `./gradlew :app:assembleDebug --no-daemon` | **PASS** | `BUILD SUCCESSFUL` |
| Release readiness | `bash scripts/validate-release-readiness.sh` | **PASS** | três manifests, assets e sidecars conferidos |
| Sintaxe shell | `bash -n scripts/*.sh rootfs-builder/*.sh` | **PASS** | nenhum erro |
| Build limpo sem warnings | `./gradlew clean test check :app:assembleDebug --no-daemon --warning-mode=all` | **PASS** | 154 tasks executadas; zero warnings |

O total reportado pelos XMLs JUnit foi de **252 execuções Kotlin/JVM/Android** nas variantes debug e release, além dos **155 testes Python**.

## Correções aplicadas nesta rodada

A suíte Android foi alinhada aos contratos atuais de segurança. Os fixtures de `AgentSandboxSessionTest` passaram a obter autorizações com tokens emitidos pelo `PolicyBroker`; comandos arbitrários continuam fora do catálogo e os testes usam capacidades allowlisted. O ciclo de execução passou a devolver o passo negado junto com os passos dependentes bloqueados, preservando a evidência completa do plano.

Também foram corrigidos os testes de limites de recursos para usar comandos Python válidos no host, o teste de integração offline passou a usar JUnit já declarado pelo módulo, e o teste do `SecurityTestLab` passou a representar explicitamente um probe incompleto sem acionar a simulação determinística automática.

Para eliminar warnings, o código substituiu APIs obsoletas de temporários e de `TarArchiveInputStream`, eliminou parâmetros e inicializações redundantes, tornou seguros os acessos a `parentFile`, removeu `extractNativeLibs` do manifesto e marcou corretamente as bibliotecas nativas pré-compiladas para preservação durante o empacotamento. O `targetSdk = 28` permanece somente no APK final, documentado como requisito operacional do proot e do domínio SELinux de compatibilidade.

## Evidência dos releases RootFS

| Release | Tamanho publicado | SHA-256 do manifesto |
|---|---:|---|
| `rootfs-v0.3.3` | `1075455791` bytes | `a43f0915d5cd6e2e0c8640b8b30855d54f7b8873ca3be95acca769100b1487f4` |
| `rootfs-agent-v0.4.1` | `1727218090` bytes | `ffe23b4bb326bfd9ef7548f6378d9ca5e6c75fce342ee7c7923203492cd4850b` |
| `rootfs-agent-android-v0.5.0` | `1764603872` bytes | `374e795ce3caa8eaf4cbb58f39d591230918f5226ec525d0140b5f94ff472c09` |

Os sidecars remotos coincidiram com os hashes declarados nos manifests.

## Limitações que permanecem

O APK foi montado e os testes unitários passaram, mas esta sessão não possui emulador ou dispositivo Android conectado. Portanto, não foram declarados como aprovados o transporte real do APK, a execução ARM64 de proot, o lifecycle em device, a instalação do RootFS, a assinatura de produção ou o comportamento sob políticas específicas de fabricantes.

O projeto também mantém as limitações arquiteturais já registradas: proot não é isolamento OS-level, `/proc` ainda expõe informações do host e a integração Android não representa todos os caminhos avançados do Brain. Essas condições não foram mascaradas pelos resultados automatizados.

## Reprodução

```bash
bash scripts/setup-test-dependencies.sh
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

python3 -m unittest discover -s tests -p 'test_*.py' -v
./gradlew test --no-daemon
./gradlew check --no-daemon
./gradlew :app:assembleDebug --no-daemon
bash scripts/validate-release-readiness.sh
bash -n scripts/*.sh rootfs-builder/*.sh
```

Para repetir a verificação específica de warnings:

```bash
./gradlew clean test check :app:assembleDebug --no-daemon --warning-mode=all
```
