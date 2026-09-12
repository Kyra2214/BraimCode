# Relatório de validação — 2026-09-12

## Ambiente

A validação foi executada no repositório `Kyra2214/BrainCode` com JDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`), Android SDK 34, Build Tools 34.0.0, Platform Tools e NDK 26.3.11579264. O Gradle utilizado pelo wrapper foi o 8.7.

## Comandos

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
python3 -m unittest discover -s tests -q
./gradlew :brain:test :android-module:test :app:test :app:assembleDebug --no-daemon
```

## Resultados

A suíte Python executou **124 testes**, todos aprovados. Os testes Kotlin/JVM dos módulos `brain`, `android-module` e `app` também foram executados com sucesso. O Gradle terminou com `BUILD SUCCESSFUL`, após 117 tarefas, incluindo a geração do APK Debug.

O artefato gerado é `app/build/outputs/apk/debug/app-debug.apk`, com aproximadamente 17 MB. Seu SHA-256 é:

```text
da680d451f1ee4157c38de5c076dd896c2e24f0d2ffc9f53956b042c1659d62d
```

## Correções encontradas durante a validação

A compilação revelou duas incompatibilidades Kotlin que foram corrigidas: a verificação de symlink no delivery foi ajustada para `Files.isSymbolicLink(file.toPath())`, e a função `isSafeHttps` foi adicionada ao módulo `app`, alinhando-o à implementação existente no módulo `brain`.

As correções foram publicadas no commit `4b409f5` (`Fix Kotlin validation and delivery compilation`) em `origin/main`.

## Limitações

Esta validação comprova compilação e testes automatizados. Não comprova ainda execução em dispositivo ou emulador Android, instalação do APK, funcionamento de RootFS/proot real, serviços Android, isolamento OS-level efetivo no dispositivo ou assinatura de release.
