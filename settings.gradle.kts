pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sandbox-mobile"

// :android-module (Fase 0.1-0.3) é Kotlin/JVM puro + a ponte Android fina
// (AndroidSandboxFactory). :app (Fase 0.4) é a tela de validação — a
// primeira coisa neste projeto que roda de verdade num device.
//
// :brain (junção Frente 1 + Frente 2) é o código Kotlin trazido do
// BraimCode original (com.brain.*): Policy/Router/Prompt/QA/Memory/
// contrato de execução. Kotlin/JVM puro, sem plugin Android — ver
// brain/build.gradle.kts e docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md.
include(":android-module")
include(":brain")
include(":app")
