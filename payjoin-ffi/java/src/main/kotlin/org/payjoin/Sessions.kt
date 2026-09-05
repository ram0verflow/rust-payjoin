package org.payjoin

import org.payjoindevkit.ReceiveSession as FfiReceiveSession
import org.payjoindevkit.ReceiverSessionOutcome as FfiReceiverSessionOutcome
import org.payjoindevkit.ReplayResult as FfiReplayResult
import org.payjoindevkit.SendSession as FfiSendSession
import org.payjoindevkit.SenderReplayResult as FfiSenderReplayResult
import org.payjoindevkit.SenderSessionOutcome as FfiSenderSessionOutcome

/**
 * Discriminated receiver session state. Java: `instanceof ReceiveSession.Initialized`.
 *
 * Variants beyond Initialized/Closed are named for type checks; protocol methods on
 * those states are not wrapped yet.
 */
sealed class ReceiveSession : AutoCloseable {
    class Initialized(val session: org.payjoin.Initialized) : ReceiveSession() {
        override fun close() {
            session.close()
        }
    }

    class UncheckedOriginalPayload internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class MaybeInputsOwned internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class MaybeInputsSeen internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class OutputsUnknown internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class WantsOutputs internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class WantsInputs internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class WantsFeeRange internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class ProvisionalProposal internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class PayjoinProposal internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class HasReplyableError internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class Monitor internal constructor(private val raw: AutoCloseable) : ReceiveSession() {
        override fun close() {
            raw.close()
        }
    }

    class ReceiverPendingFallback(val pending: org.payjoin.ReceiverPendingFallback) : ReceiveSession() {
        override fun close() {
            pending.close()
        }
    }

    class Closed(val outcome: ReceiverSessionOutcome) : ReceiveSession() {
        override fun close() {
            outcome.close()
        }
    }
}

sealed class SendSession : AutoCloseable {
    class WithReplyKey(val session: org.payjoin.WithReplyKey) : SendSession() {
        override fun close() {
            session.close()
        }
    }

    class PollingForProposal internal constructor(private val raw: AutoCloseable) : SendSession() {
        override fun close() {
            raw.close()
        }
    }

    class SenderPendingFallback(val pending: org.payjoin.SenderPendingFallback) : SendSession() {
        override fun close() {
            pending.close()
        }
    }

    class Closed(val outcome: SenderSessionOutcome) : SendSession() {
        override fun close() {
            outcome.close()
        }
    }
}

class ReceiverSessionOutcome internal constructor(
    internal val inner: FfiReceiverSessionOutcome,
) : AutoCloseable {
    override fun close() {
        inner.close()
    }
}

class SenderSessionOutcome internal constructor(
    internal val inner: FfiSenderSessionOutcome,
) : AutoCloseable {
    override fun close() {
        inner.close()
    }
}

class ReplayResult internal constructor(internal val inner: FfiReplayResult) : AutoCloseable {
    fun state(): ReceiveSession = wrapReceiveSession(inner.state())

    override fun close() {
        inner.close()
    }
}

class SenderReplayResult internal constructor(
    internal val inner: FfiSenderReplayResult,
) : AutoCloseable {
    fun state(): SendSession = wrapSendSession(inner.state())

    override fun close() {
        inner.close()
    }
}

internal fun wrapReceiveSession(raw: FfiReceiveSession): ReceiveSession =
    when (raw) {
        is FfiReceiveSession.Initialized -> ReceiveSession.Initialized(Initialized(raw.inner))
        is FfiReceiveSession.UncheckedOriginalPayload ->
            ReceiveSession.UncheckedOriginalPayload(raw.inner)
        is FfiReceiveSession.MaybeInputsOwned -> ReceiveSession.MaybeInputsOwned(raw.inner)
        is FfiReceiveSession.MaybeInputsSeen -> ReceiveSession.MaybeInputsSeen(raw.inner)
        is FfiReceiveSession.OutputsUnknown -> ReceiveSession.OutputsUnknown(raw.inner)
        is FfiReceiveSession.WantsOutputs -> ReceiveSession.WantsOutputs(raw.inner)
        is FfiReceiveSession.WantsInputs -> ReceiveSession.WantsInputs(raw.inner)
        is FfiReceiveSession.WantsFeeRange -> ReceiveSession.WantsFeeRange(raw.inner)
        is FfiReceiveSession.ProvisionalProposal -> ReceiveSession.ProvisionalProposal(raw.inner)
        is FfiReceiveSession.PayjoinProposal -> ReceiveSession.PayjoinProposal(raw.inner)
        is FfiReceiveSession.HasReplyableError -> ReceiveSession.HasReplyableError(raw.inner)
        is FfiReceiveSession.Monitor -> ReceiveSession.Monitor(raw.inner)
        is FfiReceiveSession.ReceiverPendingFallback ->
            ReceiveSession.ReceiverPendingFallback(ReceiverPendingFallback(raw.inner))
        is FfiReceiveSession.Closed -> ReceiveSession.Closed(ReceiverSessionOutcome(raw.inner))
    }

internal fun wrapSendSession(raw: FfiSendSession): SendSession =
    when (raw) {
        is FfiSendSession.WithReplyKey -> SendSession.WithReplyKey(WithReplyKey(raw.inner))
        is FfiSendSession.PollingForProposal -> SendSession.PollingForProposal(raw.inner)
        is FfiSendSession.SenderPendingFallback ->
            SendSession.SenderPendingFallback(SenderPendingFallback(raw.inner))
        is FfiSendSession.Closed -> SendSession.Closed(SenderSessionOutcome(raw.inner))
    }
