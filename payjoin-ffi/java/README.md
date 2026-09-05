# Payjoin Java Bindings

Java bindings for the [Payjoin Dev Kit](https://payjoindevkit.org/), generated from `payjoin-ffi` with [`uniffi-bindgen-java`](https://github.com/IronCoreLabs/uniffi-bindgen-java).

Payjoin lets the receiver contribute inputs to the sender's transaction. These bindings implement [BIP 78](https://github.com/bitcoin/bips/blob/master/bip-0078.mediawiki) and [BIP 77](https://github.com/bitcoin/bips/blob/master/bip-0077.md).

## Requirements

- **JDK 22+** (`javac`, `jar`). Generated code uses the Foreign Function & Memory API (Panama), which is final in 22. There is no JNA dependency.
- At runtime the JVM must allow FFM: `--enable-native-access=ALL-UNNAMED` (set for you by `./gradlew test`).
- Rust (see repo `rust-toolchain.toml` / MSRV 1.85) to build `payjoin_ffi`.
- `uniffi-bindgen-java` from the 0.4.2 error-template backport, **not** crates.io:

```shell
cargo install uniffi-bindgen-java \
  --git https://github.com/ram0verflow/uniffi-bindgen-java \
  --branch backport-0.4.2-error-templates
```

`uniffi-bindgen-java` 0.5.0 cannot read UniFFI 0.31 metadata (it fails with `Unexpected metadata type code: 116`). Released 0.4.2 emits invalid Java for this crate. The backport keeps 0.4.2's 0.31 reader and applies the 0.5.0 template fixes. Do not bump UniFFI in this crate to match 0.5.0.

Generated sources are not committed. Run generate before compile or test.

Protocol session teardown on pending-fallback and JSON persisters is `closeSession()`. `AutoCloseable.close()` drops the Rust handle (try-with-resources).

## Build and test

```shell
cd payjoin-ffi/java
bash ./scripts/generate_bindings.sh
./gradlew test
```

Or `bash ./contrib/test.sh` from this directory (uses `Cargo-recent.lock`).

`./gradlew test` includes a v2↔v2 integration test that starts a local payjoin
directory, OHTTP relay, and bitcoind. Bitcoin Core is downloaded by corepc-node
(`29_0`) on first run; it is not supplied by nix. Network access is required
the first time bitcoind is fetched.

Without nix: Rust, JDK 22+, the bindgen binary above, and the Gradle wrapper in this directory.

## Stability

Pre-1.0. Not published to Maven.

## Documentation

- [Payjoin Dev Kit](https://payjoindevkit.org/)
- [rust-payjoin](https://github.com/payjoin/rust-payjoin)
