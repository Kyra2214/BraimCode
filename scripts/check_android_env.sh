#!/usr/bin/env bash
set -euo pipefail
printf 'java='; java -version 2>&1 | head -1
if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
  echo "android_sdk=$ANDROID_HOME"
else
  echo 'android_sdk=NOT_CONFIGURED'
  echo 'Configure ANDROID_HOME or local.properties sdk.dir before Android builds.'
  exit 2
fi
