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

echo "Generating payjoin Java..."
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

OUT_DIR="java/src/main/java"
mkdir -p "$OUT_DIR"
rm -rf "$OUT_DIR/org" "$OUT_DIR/uniffi"

if ! command -v uniffi-bindgen-java >/dev/null; then
    echo "uniffi-bindgen-java not found. Install the 0.4.2 error-template backport:" >&2
    echo "  cargo install uniffi-bindgen-java \\" >&2
    echo "    --git https://github.com/ram0verflow/uniffi-bindgen-java \\" >&2
    echo "    --branch backport-0.4.2-error-templates" >&2
    exit 1
fi

# The Java generator takes the cdylib as SOURCE (there is no --library flag).
uniffi-bindgen-java generate \
    --out-dir "$OUT_DIR" \
    --no-format \
    "../target/$TARGET_PROFILE_DIR/$LIBNAME"

mkdir -p java/lib
cp "../target/$TARGET_PROFILE_DIR/$LIBNAME" "java/lib/$LIBNAME"

echo "All done!"
