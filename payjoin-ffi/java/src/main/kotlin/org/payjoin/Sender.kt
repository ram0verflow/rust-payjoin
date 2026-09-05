package org.payjoin

import java.util.concurrent.CompletableFuture
import org.payjoindevkit.InitialSendTransition as FfiInitialSendTransition
import org.payjoindevkit.SenderBuilder as FfiSenderBuilder
import org.payjoindevkit.SenderCancelTransition as FfiSenderCancelTransition
import org.payjoindevkit.SenderPendingFallback as FfiSenderPendingFallback
import org.payjoindevkit.WithReplyKey as FfiWithReplyKey

/** Builder for sender-side payjoin parameters. */
class SenderBuilder internal constructor(internal val inner: FfiSenderBuilder) : AutoCloseable {
    constructor(psbt: String, uri: PjUri) : this(wrapSenderInput { FfiSenderBuilder(psbt, uri.inner) })

    fun alwaysDisableOutputSubstitution(): SenderBuilder =
        SenderBuilder(inner.alwaysDisableOutputSubstitution())

    fun buildNonIncentivizing(minFeeRateSatPerKwu: Long): InitialSendTransition =
        InitialSendTransition(inner.buildNonIncentivizing(minFeeRateSatPerKwu.toULong()))

    fun buildRecommended(minFeeRateSatPerKwu: Long): InitialSendTransition =
        InitialSendTransition(inner.buildRecommended(minFeeRateSatPerKwu.toULong()))

    fun buildWithAdditionalFee(
        maxFeeContributionSats: Long,
        changeIndex: Byte?,
        minFeeRateSatPerKwu: Long,
        clampFeeContribution: Boolean,
    ): InitialSendTransition =
        InitialSendTransition(
            inner.buildWithAdditionalFee(
                maxFeeContributionSats.toULong(),
                changeIndex?.toUByte(),
                minFeeRateSatPerKwu.toULong(),
                clampFeeContribution,
            ),
        )

    override fun close() {
        inner.close()
    }
}

/** Persist this to obtain a [WithReplyKey] sender. */
class InitialSendTransition internal constructor(
    internal val inner: FfiInitialSendTransition,
) : AutoCloseable {
    fun save(persister: JsonSenderSessionPersister): WithReplyKey =
        WithReplyKey(inner.save(persister.asUniFfi()))

    fun saveAsync(persister: JsonSenderSessionPersisterAsync): CompletableFuture<WithReplyKey> =
        runAsync { WithReplyKey(inner.saveAsync(persister.asUniFfi())) }

    override fun close() {
        inner.close()
    }
}

/** Sender session after building a recommended request. */
class WithReplyKey internal constructor(internal val inner: FfiWithReplyKey) : AutoCloseable {
    fun cancel(): SenderCancelTransition = SenderCancelTransition(inner.cancel())

    override fun close() {
        inner.close()
    }
}

class SenderCancelTransition internal constructor(
    internal val inner: FfiSenderCancelTransition,
) : AutoCloseable {
    fun save(persister: JsonSenderSessionPersister): SenderPendingFallback =
        SenderPendingFallback(inner.save(persister.asUniFfi()))

    fun saveAsync(
        persister: JsonSenderSessionPersisterAsync,
    ): CompletableFuture<SenderPendingFallback> =
        runAsync { SenderPendingFallback(inner.saveAsync(persister.asUniFfi())) }

    override fun close() {
        inner.close()
    }
}

/**
 * Sender fallback after cancel. [closeSession] is protocol teardown;
 * [close] drops the Rust handle.
 */
class SenderPendingFallback internal constructor(
    internal val inner: FfiSenderPendingFallback,
) : AutoCloseable {
    fun fallbackTx(): ByteArray = inner.fallbackTx()

    fun closeSession(): BroadcastedTransition = BroadcastedTransition(inner.closeSession())

    override fun close() {
        inner.close()
    }
}
