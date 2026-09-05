#!/usr/bin/env bash
set -euo pipefail

# Generate UniFFI Kotlin first; this package wraps it.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "$SCRIPT_DIR/../../kotlin/scripts/generate_bindings.sh"
