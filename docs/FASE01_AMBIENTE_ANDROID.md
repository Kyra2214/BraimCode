# Fase 1 — Ambiente Android

## Objetivo

Definir uma configuração reproduzível para compilar e validar os módulos Android do BrainCode com SDK 34, Java 17 e o runtime `proot` quando os artefatos nativos estiverem disponíveis.

## Requisitos

| Componente | Requisito |
|---|---|
| JDK | 17 |
| Android SDK | API 34 |
| Build tools | 34.0.0 ou compatível |
| Android Gradle Plugin | 8.5.0 |
| Kotlin | 1.9.24 |
| ABI | arm64-v8a |
| NDK | Opcional; necessário apenas para artefatos nativos |
| RootFS/proot | Dependência de implantação, não gerada pelo Gradle |

## Configuração

```bash
cp android-sdk.env.example android-sdk.env
source android-sdk.env
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
./gradlew --version
./gradlew :brain:test :android-module:test :app:assembleDebug
```

O arquivo `android-sdk.env` é local e não deve ser versionado. Em máquinas que não usam `ANDROID_HOME` padrão, o caminho pode ser definido diretamente antes da compilação.

## Validação desta fase

A configuração do projeto já declara `compileSdk = 34`, Java/Kotlin 17 e ABI `arm64-v8a`. A validação automática deve distinguir claramente duas situações: compilação concluída, ou ambiente ausente. Não se deve marcar APK como validado apenas porque os contratos Kotlin compilam.

No ambiente de desenvolvimento sem SDK Android, a Fase 1 é considerada **configurada/documentada**, e a validação de APK permanece pendente até a instalação dos pacotes acima.
