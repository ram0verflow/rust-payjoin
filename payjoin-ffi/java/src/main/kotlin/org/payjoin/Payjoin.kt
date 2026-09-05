package org.payjoin

import java.util.concurrent.CompletableFuture

/**
 * UniFFI free functions. From Java: `Payjoin.replayReceiverEventLog(persister)`.
 */
object Payjoin {
    @JvmStatic
    @Throws(ReceiverReplayException::class)
    fun replayReceiverEventLog(persister: JsonReceiverSessionPersister): ReplayResult =
        wrapReceiverReplay {
            ReplayResult(org.payjoindevkit.replayReceiverEventLog(persister.asUniFfi()))
        }

    @JvmStatic
    fun replayReceiverEventLogAsync(
        persister: JsonReceiverSessionPersisterAsync,
    ): CompletableFuture<ReplayResult> =
        runAsync {
            wrapReceiverReplay {
                ReplayResult(org.payjoindevkit.replayReceiverEventLogAsync(persister.asUniFfi()))
            }
        }

    @JvmStatic
    @Throws(SenderReplayException::class)
    fun replaySenderEventLog(persister: JsonSenderSessionPersister): SenderReplayResult =
        wrapSenderReplay {
            SenderReplayResult(org.payjoindevkit.replaySenderEventLog(persister.asUniFfi()))
        }

    @JvmStatic
    fun replaySenderEventLogAsync(
        persister: JsonSenderSessionPersisterAsync,
    ): CompletableFuture<SenderReplayResult> =
        runAsync {
            wrapSenderReplay {
                SenderReplayResult(org.payjoindevkit.replaySenderEventLogAsync(persister.asUniFfi()))
            }
        }
}

