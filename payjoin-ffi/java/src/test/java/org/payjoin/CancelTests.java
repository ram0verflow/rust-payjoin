package org.payjoin;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CancelTests {
    private static final byte[] OHTTP_KEYS = hex(
            "01001604ba48c49c3d4a92a3ad00ecc63a024da10ced02180c73ec12d8a7ad2cc91bb483824fe2bee8d28bfe2eb2fc6453bc4d31cd851e8a6540e86c5382af588d370957000400010003");

    @Test
    void receiverCancelFromInitialized() {
        InMemoryReceiverPersister persister = new InMemoryReceiverPersister();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized initialized = new ReceiverBuilder(
                        "tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
                .build()
                .save(persister);
        ReceiverPendingFallback fallbackTx = initialized.cancel().save(persister);
        assertNull(fallbackTx);
        try (ReplayResult result = Payjoin.replayReceiverEventLog(persister)) {
            assertInstanceOf(ReceiveSession.Closed.class, result.state());
        }
    }

    @Test
    void receiverCancelFromInitializedAsync() throws Exception {
        InMemoryReceiverPersisterAsync persister = new InMemoryReceiverPersisterAsync();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized initialized = new ReceiverBuilder(
                        "tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
                .build()
                .saveAsync(persister)
                .get();
        ReceiverPendingFallback fallbackTx = initialized.cancel().saveAsync(persister).get();
        assertNull(fallbackTx);
        try (ReplayResult result = Payjoin.replayReceiverEventLogAsync(persister).get()) {
            assertInstanceOf(ReceiveSession.Closed.class, result.state());
        }
    }

    @Test
    void senderCancelFromWithReplyKey() {
        InMemoryReceiverPersister receiverPersister = new InMemoryReceiverPersister();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized receiver = new ReceiverBuilder(
                        "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK", "https://example.com", ohttpKeys)
                .build()
                .save(receiverPersister);
        PjUri uri = receiver.pjUri();
        InMemorySenderPersister senderPersister = new InMemorySenderPersister();
        WithReplyKey withReplyKey =
                new SenderBuilder(TestUtils.originalPsbt(), uri).buildRecommended(1000).save(senderPersister);
        SenderPendingFallback pendingFallback = withReplyKey.cancel().save(senderPersister);
        assertNotNull(pendingFallback);
        assertTrue(pendingFallback.fallbackTx().length > 0);
        try (SenderReplayResult cancelled = Payjoin.replaySenderEventLog(senderPersister)) {
            assertInstanceOf(SendSession.SenderPendingFallback.class, cancelled.state());
        }
        pendingFallback.closeSession().save(senderPersister);
        try (SenderReplayResult closed = Payjoin.replaySenderEventLog(senderPersister)) {
            assertInstanceOf(SendSession.Closed.class, closed.state());
        }
    }

    @Test
    void senderCancelFromWithReplyKeyAsync() throws Exception {
        InMemoryReceiverPersisterAsync receiverPersister = new InMemoryReceiverPersisterAsync();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized receiver = new ReceiverBuilder(
                        "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK", "https://example.com", ohttpKeys)
                .build()
                .saveAsync(receiverPersister)
                .get();
        PjUri uri = receiver.pjUri();
        InMemorySenderPersisterAsync senderPersister = new InMemorySenderPersisterAsync();
        WithReplyKey withReplyKey = new SenderBuilder(TestUtils.originalPsbt(), uri)
                .buildRecommended(1000)
                .saveAsync(senderPersister)
                .get();
        SenderPendingFallback pendingFallback = withReplyKey.cancel().saveAsync(senderPersister).get();
        assertNotNull(pendingFallback);
        assertTrue(pendingFallback.fallbackTx().length > 0);
        try (SenderReplayResult cancelled = Payjoin.replaySenderEventLogAsync(senderPersister).get()) {
            assertInstanceOf(SendSession.SenderPendingFallback.class, cancelled.state());
        }
        pendingFallback.closeSession().saveAsync(senderPersister).get();
        try (SenderReplayResult closed = Payjoin.replaySenderEventLogAsync(senderPersister).get()) {
            assertInstanceOf(SendSession.Closed.class, closed.state());
        }
    }

    private static byte[] hex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
