package org.payjoindevkit

import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

private val ohttpKeysData =
    "01001604ba48c49c3d4a92a3ad00ecc63a024da10ced02180c73ec12d8a7ad2cc91bb483824fe2bee8d28bfe2eb2fc6453bc4d31cd851e8a6540e86c5382af588d370957000400010003"
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

class PersistenceTests {
    @Test
    fun receiverPersistence() {
        val persister = InMemoryReceiverPersister()
        val ohttpKeys = OhttpKeys.decode(ohttpKeysData)
        ReceiverBuilder("tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
            .build()
            .save(persister)
        val state = replayReceiverEventLog(persister).state()
        assertIs<ReceiveSession.Initialized>(state)
    }

    @Test
    fun senderPersistence() {
        val receiverPersister = InMemoryReceiverPersister()
        val ohttpKeys = OhttpKeys.decode(ohttpKeysData)
        val receiver = ReceiverBuilder(
            "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK",
            "https://example.com",
            ohttpKeys,
        ).build().save(receiverPersister)
        val uri = receiver.pjUri()
        val senderPersister = InMemorySenderPersister()
        SenderBuilder(originalPsbt(), uri).buildRecommended(1000u).save(senderPersister)
        val state = replaySenderEventLog(senderPersister).state()
        assertIs<SendSession.WithReplyKey>(state)
    }

    @Test
    fun receiverPersistenceAsync() = runTest {
        val persister = InMemoryReceiverPersisterAsync()
        val ohttpKeys = OhttpKeys.decode(ohttpKeysData)
        ReceiverBuilder("tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
            .build()
            .saveAsync(persister)
        val state = replayReceiverEventLogAsync(persister).state()
        assertIs<ReceiveSession.Initialized>(state)
    }

    @Test
    fun senderPersistenceAsync() = runTest {
        val receiverPersister = InMemoryReceiverPersisterAsync()
        val ohttpKeys = OhttpKeys.decode(ohttpKeysData)
        val receiver = ReceiverBuilder(
            "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK",
            "https://example.com",
            ohttpKeys,
        ).build().saveAsync(receiverPersister)
        val uri = receiver.pjUri()
        val senderPersister = InMemorySenderPersisterAsync()
        SenderBuilder(originalPsbt(), uri).buildRecommended(1000u).saveAsync(senderPersister)
        val state = replaySenderEventLogAsync(senderPersister).state()
        assertIs<SendSession.WithReplyKey>(state)
    }
}
