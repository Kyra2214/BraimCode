#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"

# Baseline explícita: estes são os chamadores de produção existentes no
# momento em que o gate foi introduzido. Testes ficam fora da varredura.
declare -A ALLOWED
while IFS= read -r path; do ALLOWED["$path"]=1; done <<'EOF'
brain/src/main/kotlin/com/brain/qa/ExecutorValidacaoProjeto.kt
brain/src/main/kotlin/com/brain/qa/DescobertaComandosValidacao.kt
android-module/src/main/kotlin/com/sandbox/runtime/ProotProcessLauncher.kt
android-module/src/main/kotlin/com/sandbox/runtime/ManagedSandboxRuntime.kt
android-module/src/main/kotlin/com/sandbox/runtime/SandboxRuntime.kt
android-module/src/main/kotlin/com/sandbox/agent/AgentSandboxSession.kt
app/src/main/kotlin/com/sandbox/sandbox/ToolchainModels.kt
app/src/main/kotlin/com/sandbox/sandbox/TestLab.kt
app/src/main/kotlin/com/sandbox/sandbox/Services.kt
app/src/main/kotlin/com/sandbox/sandbox/Security.kt
app/src/main/kotlin/com/sandbox/sandbox/PluginModels.kt
android-module/src/main/kotlin/com/sandbox/runtime/PackagedRuntime.kt
app/src/main/kotlin/com/sandbox/sandbox/GitManager.kt
app/src/main/kotlin/com/sandbox/app/SandboxViewModel.kt
brain/src/main/kotlin/com/brain/execution/BrainExecutionCoordinator.kt
android-module/src/main/kotlin/com/sandbox/android/AndroidSandboxFactory.kt
EOF

status=0
while IFS=: read -r file line text; do
  rel="${file#"$ROOT/"}"
  [[ "$rel" == */src/test/* ]] && continue
  [[ -n "${ALLOWED[$rel]:-}" ]] && continue
  printf 'UNAUTHORIZED_DIRECT_EXECUTION %s:%s:%s\n' "$rel" "$line" "$text" >&2
  status=1
done < <(rg -n --glob '*.kt' --glob '*.java' --glob '!**/build/**' --glob '!**/.gradle/**' '\.execute\(|ProcessBuilder\(|\.launch\(\s*(command|listOf)' "$ROOT/android-module" "$ROOT/app" "$ROOT/brain" || true)

if (( status != 0 )); then exit 1; fi
echo 'architecture gate passed'
