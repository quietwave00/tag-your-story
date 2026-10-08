#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
    printf '[verify] %s\n' "$1"
}

log "environment"

if ! command -v java >/dev/null 2>&1; then
    printf '[verify] java not found in PATH\n' >&2
    exit 1
fi

if [ ! -x "./gradlew" ]; then
    printf '[verify] ./gradlew is missing or not executable\n' >&2
    exit 1
fi

log "gradle check"
./gradlew --no-daemon check

log "architecture tests: not configured"
log "formatter/linter: not configured"
log "migration validation: not configured"
