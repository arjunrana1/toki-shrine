#!/usr/bin/env bash
# Gradle wrapper with the toolchain pre-set. Use this instead of ./gradlew.
#   ./build.sh assembleDebug
set -euo pipefail
cd "$(dirname "$0")"
source tools/env.sh
exec ./gradlew "$@"
