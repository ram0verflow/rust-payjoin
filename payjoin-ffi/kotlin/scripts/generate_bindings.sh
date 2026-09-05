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
FEATURE_ARGS=()
if [[ -n $PAYJOIN_FFI_FEATURES ]]; then
    FEATURE_ARGS=(--features "$PAYJOIN_FFI_FEATURES")
fi

cargo build "${FEATURE_ARGS[@]}" --profile "$PAYJOIN_FFI_PROFILE" -p payjoin-ffi

OUT_DIR="kotlin/src/main/kotlin"
mkdir -p "$OUT_DIR"
rm -rf "$OUT_DIR/org"

# ktlint is optional; --no-format keeps generate working without it.
cargo run "${FEATURE_ARGS[@]}" --profile dev -p payjoin-ffi --bin uniffi-bindgen -- generate \
    --library "../target/$TARGET_PROFILE_DIR/$LIBNAME" \
    --language kotlin \
    --out-dir "$OUT_DIR" \
    --no-format

mkdir -p kotlin/lib
cp "../target/$TARGET_PROFILE_DIR/$LIBNAME" "kotlin/lib/$LIBNAME"

echo "All done!"
