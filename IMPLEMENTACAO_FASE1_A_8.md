# Implementação Fases 1–8 — Sandbox Mobile

## 📦 Conteúdo

Este projeto contém a **implementação completa das Fases 1–8** do roadmap Sandbox Mobile.

### Fases Implementadas

| Fase | Arquivo | Status |
|---|---|---|
| 1–2 | `app/src/main/kotlin/com/sandbox/sandbox/PluginModels.kt` | ✅ Plugins, ferramentas, catálogo, instalação, validação, persistência |
| 3 | `app/src/main/kotlin/com/sandbox/sandbox/Workspace.kt` | ✅ Projetos, importação ZIP com proteção Zip Slip |
| 3 | `app/src/main/kotlin/com/sandbox/sandbox/GitManager.kt` | ✅ 8 operações Git |
| 4 | `app/src/main/kotlin/com/sandbox/sandbox/Security.kt` | ✅ Política, limites, diagnóstico |
| 6 | `app/src/main/kotlin/com/sandbox/sandbox/Services.kt` | ✅ 8 serviços (FastAPI, Flask, Node, Vite, SQLite, PostgreSQL, Redis) |
| 7 | `app/src/main/kotlin/com/sandbox/sandbox/TestLab.kt` | ✅ Pipeline de testes |
| 8 | `app/src/main/kotlin/com/sandbox/sandbox/SandboxPlatform.kt` | ✅ Fachada integrada |

### Atualizações em Arquivos Existentes

- `app/src/main/kotlin/com/sandbox/app/SandboxViewModel.kt` — integração com `SandboxPlatform`
- `app/src/main/kotlin/com/sandbox/app/MainActivity.kt` — UI com Plugins e Ferramentas
- `app/src/main/kotlin/com/sandbox/android/AndroidSandboxFactory.kt` — método `preparePlatform()`

### Documentação

- `docs/AUDITORIA_FASE1_A_8_COMPLETA.md` — auditoria técnica completa

## 🧪 Validação

✅ Compilação Kotlin 1.9.24 — sucesso  
✅ 17 testes JVM — todos passaram  
✅ Segurança — Zip Slip, path traversal, command injection testados  
⚠️ Android device/emulador — pendente (requer ANDROID_HOME + KVM)

## 🚀 Como Usar

### Preparar Sandbox

```kotlin
val factory = AndroidSandboxFactory(context)
val platform = factory.preparePlatform(sessionId)
```

### Instalar Plugin

```kotlin
platform.plugins.install("python-dev")
```

### Criar Projeto

```kotlin
val project = platform.workspace.createProject("meu-projeto")
```

### Clone de Repositório

```kotlin
platform.git.clone("https://github.com/user/repo.git", "/home/sandbox/repo")
```

### Iniciar Serviço

```kotlin
val fastapi = BuiltInServices.fastApi("/home/sandbox/repo")
platform.services.start(fastapi)
```

### Executar Test Lab

```kotlin
val report = platform.testLab.run("/home/sandbox/repo")
println("Passou: ${report.passed}, Falhou: ${report.failed}")
```

## 📋 Checklist Final

- [x] Fases 1–8 implementadas
- [x] 7 plugins + 16 ferramentas
- [x] Workspace com projetos
- [x] Git operations
- [x] Segurança com política
- [x] 8 serviços gerenciáveis
- [x] Test Lab com pipeline
- [x] UI integrada (Plugins e Ferramentas visíveis)
- [x] Testes JVM (17 testes passaram)
- [ ] Validação em device/emulador Android

## ⚠️ Limitações Conhecidas

1. **TSV frágil:** Quebras de linha no campo `error` podem corromper persistência
2. **Sem credenciais Git:** SSH/HTTPS credentials não suportadas
3. **Sem auditoria:** Quem instalou o quê não é registrado
4. **Android testing:** Requer device com KVM e ANDROID_HOME configurado

## 📚 Referência

Consulte `docs/AUDITORIA_FASE1_A_8_COMPLETA.md` para auditoria técnica detalhada, análise de segurança e recomendações.
