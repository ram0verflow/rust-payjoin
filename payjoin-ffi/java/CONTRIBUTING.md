# Contributing to the Payjoin Java Bindings

Java bindings for the [Payjoin Dev Kit](https://payjoindevkit.org/), generated from `payjoin-ffi` with [`uniffi-bindgen-java`](https://github.com/IronCoreLabs/uniffi-bindgen-java) (Foreign Function & Memory API, not JNA).

## Development

```shell
git clone https://github.com/payjoin/rust-payjoin.git
cd rust-payjoin/payjoin-ffi/java
cargo install uniffi-bindgen-java \
  --git https://github.com/ram0verflow/uniffi-bindgen-java \
  --branch backport-0.4.2-error-templates
bash ./scripts/generate_bindings.sh
./gradlew test
```

Generation uses the installed `uniffi-bindgen-java` binary. There is no extra Cargo feature on `payjoin-ffi`. By default, development generation enables `_test-utils`. For production bindings, set `PAYJOIN_FFI_FEATURES` to empty:

```shell
PAYJOIN_FFI_FEATURES= bash ./scripts/generate_bindings.sh
```

Then `./gradlew compileJava` to confirm the shipped API does not depend on `_test-utils`.

Protocol `close` is renamed to `closeSession` only in `[bindings.java.rename]` in `payjoin-ffi/uniffi.toml`, so it does not clash with `AutoCloseable.close()`. Java cannot overload methods that differ only by return type.

`./gradlew test` passes `--enable-native-access=ALL-UNNAMED` and points the FFM loader at `java/lib/` via `uniffi.component.payjoin.libraryOverride`.
