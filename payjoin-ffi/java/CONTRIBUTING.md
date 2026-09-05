# Contributing to the Payjoin Java Bindings

`org.payjoin` is a Java API over the UniFFI Kotlin bindings in [`../kotlin`](../kotlin). Callers should depend on `org.payjoin`, not `org.payjoindevkit`.

## Development

```shell
git clone https://github.com/payjoin/rust-payjoin.git
cd rust-payjoin/payjoin-ffi/java
bash ./scripts/generate_bindings.sh
./gradlew test
```

Generate runs `../kotlin/scripts/generate_bindings.sh` first. Default generation enables `_test-utils`. For production bindings:

```shell
PAYJOIN_FFI_FEATURES= bash ./scripts/generate_bindings.sh
```

Protocol `close` is `closeSession()` on pending-fallback and JSON persisters, matching `[bindings.kotlin.rename]` in `payjoin-ffi/uniffi.toml`. `AutoCloseable.close()` drops the Rust handle.

Use a local JDK 21+ and the Gradle wrapper in this directory.
