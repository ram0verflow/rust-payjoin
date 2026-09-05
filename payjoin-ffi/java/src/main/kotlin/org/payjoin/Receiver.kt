package org.payjoin

import java.util.concurrent.CompletableFuture
import org.payjoindevkit.BroadcastedTransition as FfiBroadcastedTransition
import org.payjoindevkit.CancelTransition as FfiCancelTransition
import org.payjoindevkit.InitialReceiveTransition as FfiInitialReceiveTransition
import org.payjoindevkit.Initialized as FfiInitialized
import org.payjoindevkit.PendingFallbackTransition as FfiPendingFallbackTransition
import org.payjoindevkit.ReceiverBuilder as FfiReceiverBuilder
import org.payjoindevkit.ReceiverPendingFallback as FfiReceiverPendingFallback

/** Builder for a BIP77 receiver session. */
class ReceiverBuilder internal constructor(internal val inner: FfiReceiverBuilder) : AutoCloseable {
    constructor(address: String, directory: String, ohttpKeys: OhttpKeys) : this(
        wrapReceiverBuilder { FfiReceiverBuilder(address, directory, ohttpKeys.inner) },
    )

    fun build(): InitialReceiveTransition = InitialReceiveTransition(inner.build())

    fun withAmount(amountSats: Long): ReceiverBuilder =
        ReceiverBuilder(inner.withAmount(amountSats.toULong()))

    fun withExpiration(expirationSecs: Long): ReceiverBuilder =
        ReceiverBuilder(inner.withExpiration(expirationSecs.toULong()))

    fun withMaxFeeRate(maxEffectiveFeeRateSatPerVb: Long): ReceiverBuilder =
        ReceiverBuilder(inner.withMaxFeeRate(maxEffectiveFeeRateSatPerVb.toULong()))

    override fun close() {
        inner.close()
    }
}

/** Persist this to obtain an [Initialized] receiver. */
class InitialReceiveTransition internal constructor(
    internal val inner: FfiInitialReceiveTransition,
) : AutoCloseable {
    fun save(persister: JsonReceiverSessionPersister): Initialized =
        Initialized(inner.save(persister.asUniFfi()))

    fun saveAsync(persister: JsonReceiverSessionPersisterAsync): CompletableFuture<Initialized> =
        runAsync { Initialized(inner.saveAsync(persister.asUniFfi())) }

    override fun close() {
        inner.close()
    }
}

/** An initialized receiver session. */
class Initialized internal constructor(internal val inner: FfiInitialized) : AutoCloseable {
    fun cancel(): CancelTransition = CancelTransition(inner.cancel())

    fun pjUri(): PjUri = PjUri(inner.pjUri())

    override fun close() {
        inner.close()
    }
}

/** Persist to cancel a receiver session. */
class CancelTransition internal constructor(
    internal val inner: FfiCancelTransition,
) : AutoCloseable {
    fun save(persister: JsonReceiverSessionPersister): ReceiverPendingFallback? =
        inner.save(persister.asUniFfi())?.let { ReceiverPendingFallback(it) }

    fun saveAsync(
        persister: JsonReceiverSessionPersisterAsync,
    ): CompletableFuture<ReceiverPendingFallback?> =
        runAsync { inner.saveAsync(persister.asUniFfi())?.let { ReceiverPendingFallback(it) } }

    override fun close() {
        inner.close()
    }
}

/**
 * Receiver fallback after cancel. [closeSession] is protocol teardown;
 * [close] drops the Rust handle.
 */
class ReceiverPendingFallback internal constructor(
    internal val inner: FfiReceiverPendingFallback,
) : AutoCloseable {
    fun fallbackTx(): ByteArray = inner.fallbackTx()

    fun closeSession(): PendingFallbackTransition = PendingFallbackTransition(inner.closeSession())

    override fun close() {
        inner.close()
    }
}

class PendingFallbackTransition internal constructor(
    internal val inner: FfiPendingFallbackTransition,
) : AutoCloseable {
    fun save(persister: JsonReceiverSessionPersister) {
        inner.save(persister.asUniFfi())
    }

    fun saveAsync(persister: JsonReceiverSessionPersisterAsync): CompletableFuture<Void> =
        runAsync {
            inner.saveAsync(persister.asUniFfi())
        }.thenCompose { CompletableFuture.completedFuture(null) }

    override fun close() {
        inner.close()
    }
}

class BroadcastedTransition internal constructor(
    internal val inner: FfiBroadcastedTransition,
) : AutoCloseable {
    fun save(persister: JsonSenderSessionPersister) {
        inner.save(persister.asUniFfi())
    }

    fun saveAsync(persister: JsonSenderSessionPersisterAsync): CompletableFuture<Void> =
        runAsync {
            inner.saveAsync(persister.asUniFfi())
        }.thenCompose { CompletableFuture.completedFuture(null) }

    override fun close() {
        inner.close()
    }
}
