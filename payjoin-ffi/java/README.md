# Payjoin Java Bindings

Java language bindings for the [Payjoin Dev Kit](https://payjoindevkit.org/), package `org.payjoin`.

Payjoin lets the receiver contribute inputs to the sender's transaction. These bindings implement [BIP 78](https://github.com/bitcoin/bips/blob/master/bip-0078.mediawiki) and [BIP 77](https://github.com/bitcoin/bips/blob/master/bip-0077.md).

Requires **JDK 21+**. Native `payjoin_ffi` is loaded via JNA. The public API is Java; generate uses the UniFFI Kotlin backend in [`../kotlin`](../kotlin) and this package wraps it (`Uri.parse`, `long`, `List`, `CompletableFuture`).

`AutoCloseable.close()` drops the Rust handle (try-with-resources). Protocol session teardown on pending-fallback and JSON persisters is `closeSession()`.

URI parse, receiver and sender persist/replay, and cancel from initialized and with-reply-key are implemented. Other session variants exist for `instanceof` checks; their protocol methods are not wrapped yet.

## Build and test

```shell
cd payjoin-ffi/java
bash ./scripts/generate_bindings.sh
./gradlew test
```

Or `bash ./contrib/test.sh` from this directory (uses `Cargo-recent.lock`).

`payjoin-ffi/contrib/test.sh` runs Java after the other language bindings. Java generate
rewrites the Kotlin sources, so it must not run in parallel with `kotlin/`.

## Stability

Pre-1.0. Generated Kotlin is not committed; run generate before test or pack.

## Documentation

- [Payjoin Dev Kit](https://payjoindevkit.org/)
- [rust-payjoin](https://github.com/payjoin/rust-payjoin)
- [`CONTRIBUTING.md`](CONTRIBUTING.md) to build from source
