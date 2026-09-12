#!/usr/bin/env bash
set -euo pipefail

OS=$(uname -s)
echo "Running on $OS"

if [[ $OS == "Darwin" ]]; then
    LIBNAME=libpayjoin_ffi.dylib
elif [[ $OS == "Linux" ]]; then
    LIBNAME=libpayjoin_ffi.so
elif [[ $OS == MINGW* || $OS == MSYS* || $OS == CYGWIN* ]]; then
    LIBNAME=payjoin_ffi.dll
else
    echo "Unsupported os: $OS"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../.."

echo "Generating payjoin Kotlin..."
PAYJOIN_FFI_FEATURES=${PAYJOIN_FFI_FEATURES-_test-utils}
PAYJOIN_FFI_PROFILE=${PAYJOIN_FFI_PROFILE:-dev}
if [[ $PAYJOIN_FFI_PROFILE == "dev" ]]; then
    TARGET_PROFILE_DIR=debug
else
    TARGET_PROFILE_DIR=$PAYJOIN_FFI_PROFILE
fi
# Empty FEATURE_ARGS + `set -u` is unbound on macOS bash 3.2. Pass --features only when set.
run_cargo() {
    local cmd=$1
    shift
    if [[ -n $PAYJOIN_FFI_FEATURES ]]; then
        cargo "$cmd" --features "$PAYJOIN_FFI_FEATURES" "$@"
    else
        cargo "$cmd" "$@"
    fi
}

run_cargo build --profile "$PAYJOIN_FFI_PROFILE" -p payjoin-ffi

# Cargo writes here when CARGO_TARGET_DIR is set; otherwise the workspace target/.
TARGET_ROOT="${CARGO_TARGET_DIR:-../target}"
NATIVE_LIB="$TARGET_ROOT/$TARGET_PROFILE_DIR/$LIBNAME"

OUT_DIR="kotlin/src/main/kotlin"
mkdir -p "$OUT_DIR"
rm -rf "$OUT_DIR/org"

# ktlint is optional; --no-format keeps generate working without it.
run_cargo run --profile dev -p payjoin-ffi --bin uniffi-bindgen -- generate \
    --library "$NATIVE_LIB" \
    --language kotlin \
    --out-dir "$OUT_DIR" \
    --no-format

mkdir -p kotlin/lib
cp "$NATIVE_LIB" "kotlin/lib/$LIBNAME"

echo "All done!"
