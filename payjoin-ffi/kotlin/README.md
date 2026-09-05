# Payjoin Kotlin Bindings

Kotlin/JVM bindings for the [Payjoin Dev Kit](https://payjoindevkit.org/), generated from `payjoin-ffi` with UniFFI (`--language kotlin`).

Payjoin lets the receiver contribute inputs to the sender's transaction. These bindings implement [BIP 78](https://github.com/bitcoin/bips/blob/master/bip-0078.mediawiki) and [BIP 77](https://github.com/bitcoin/bips/blob/master/bip-0077.md).

Requires **JDK 21+**. Native `payjoin_ffi` is loaded via JNA.

Protocol session teardown on pending-fallback and JSON persisters is `closeSession()`. `AutoCloseable.close()` drops the Rust handle (try-with-resources / `.use`). Other language bindings keep `close`.

## Build and test

```shell
cd payjoin-ffi/kotlin
bash ./scripts/generate_bindings.sh
./gradlew test
```

Or `bash ./contrib/test.sh` from this directory (uses `Cargo-recent.lock`).

`./gradlew test` includes a v2↔v2 integration test that starts a local payjoin directory, OHTTP relay, and bitcoind. Bitcoin Core is downloaded by corepc-node (`29_0`) on first run; it is not supplied by nix.

Without nix: Rust (see repo `rust-toolchain.toml` / MSRV 1.85), JDK 21+, and the Gradle wrapper in this directory. Network access is required the first time bitcoind is fetched.

## Stability

Pre-1.0. Generated sources are not committed; run generate before test or pack.

## Documentation

- [Payjoin Dev Kit](https://payjoindevkit.org/)
- [rust-payjoin](https://github.com/payjoin/rust-payjoin)
