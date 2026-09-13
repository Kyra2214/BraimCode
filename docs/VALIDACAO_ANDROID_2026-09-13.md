# Validação Android e Gradle — 2026-09-13

## Escopo

A validação foi executada em uma cópia limpa clonada diretamente de `origin/main`, inicialmente no commit `49e20f9` e finalizada no commit `bb5a021`. O objetivo foi validar os testes dependentes do Android SDK e confirmar a geração do APK Debug.

## Ambiente

Foram usados JDK 17 em `/usr/lib/jvm/java-17-openjdk-amd64`, Android SDK em `/home/ubuntu/Android/Sdk`, compile/target SDK 34, Build Tools 34.0.0, Platform Tools e NDK 26.3.11579264. O Gradle wrapper do projeto usa Gradle 8.7.

## Comandos executados

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

python3 -m unittest discover -s tests -q
./gradlew :brain:test --no-daemon --rerun-tasks
./gradlew :android-module:test :app:test :app:assembleDebug --no-daemon --rerun-tasks
```

## Resultado

A suíte Python passou com **134 testes**. O módulo `:brain` compilou e passou nos testes. Os testes de `:android-module` e `:app` passaram depois de duas correções necessárias encontradas no clone atualizado. A geração do APK terminou com `BUILD SUCCESSFUL`, com 115 tarefas executadas.

A primeira tentativa revelou que o helper `passo` de `CicloExecucaoPlanoTest` não encaminhava o argumento `riskClass` para `PassoPlano`. Também revelou que o módulo `app` usava diretamente classes de `:brain` sem declarar a dependência direta. Essas correções foram publicadas no commit `bb5a021`.

O APK validado é `app/build/outputs/apk/debug/app-debug.apk`, com aproximadamente 17 MB e SHA-256:

```text
e51adf29e818ec073dc89cea247ae6c50f1652a6673995cb6a21ac355bb3b93e
```

## Pendências do escopo Android offline

A validação comprova testes JVM, compilação Android e empacotamento do APK Debug. Para fechar o escopo atual Android offline ainda falta instalação ou execução em device/emulador ARM64, execução real de RootFS/proot, lifecycle sob interrupção e desempenho. Assinatura de release, keystore, infraestrutura OS-level de servidor e backends distribuídos são extensões futuras e não fazem parte do fechamento atual.

Há warnings não bloqueantes sobre o uso depreciado de `createTempDir` nos testes Kotlin e sobre a busca de um caminho alternativo de JDK 17 pelo Gradle.
