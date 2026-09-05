package org.payjoin;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class PersistenceTests {
    private static final byte[] OHTTP_KEYS = hex(
            "01001604ba48c49c3d4a92a3ad00ecc63a024da10ced02180c73ec12d8a7ad2cc91bb483824fe2bee8d28bfe2eb2fc6453bc4d31cd851e8a6540e86c5382af588d370957000400010003");

    @Test
    void receiverPersistence() {
        InMemoryReceiverPersister persister = new InMemoryReceiverPersister();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        new ReceiverBuilder("tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
                .build()
                .save(persister);
        try (ReplayResult result = Payjoin.replayReceiverEventLog(persister)) {
            assertInstanceOf(ReceiveSession.Initialized.class, result.state());
        }
    }

    @Test
    void senderPersistence() {
        InMemoryReceiverPersister receiverPersister = new InMemoryReceiverPersister();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized receiver = new ReceiverBuilder(
                        "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK", "https://example.com", ohttpKeys)
                .build()
                .save(receiverPersister);
        PjUri uri = receiver.pjUri();
        InMemorySenderPersister senderPersister = new InMemorySenderPersister();
        new SenderBuilder(TestUtils.originalPsbt(), uri).buildRecommended(1000).save(senderPersister);
        try (SenderReplayResult result = Payjoin.replaySenderEventLog(senderPersister)) {
            assertInstanceOf(SendSession.WithReplyKey.class, result.state());
        }
    }

    @Test
    void receiverPersistenceAsync() throws Exception {
        InMemoryReceiverPersisterAsync persister = new InMemoryReceiverPersisterAsync();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        new ReceiverBuilder("tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", ohttpKeys)
                .build()
                .saveAsync(persister)
                .get();
        try (ReplayResult result = Payjoin.replayReceiverEventLogAsync(persister).get()) {
            assertInstanceOf(ReceiveSession.Initialized.class, result.state());
        }
    }

    @Test
    void senderPersistenceAsync() throws Exception {
        InMemoryReceiverPersisterAsync receiverPersister = new InMemoryReceiverPersisterAsync();
        OhttpKeys ohttpKeys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized receiver = new ReceiverBuilder(
                        "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK", "https://example.com", ohttpKeys)
                .build()
                .saveAsync(receiverPersister)
                .get();
        PjUri uri = receiver.pjUri();
        InMemorySenderPersisterAsync senderPersister = new InMemorySenderPersisterAsync();
        new SenderBuilder(TestUtils.originalPsbt(), uri)
                .buildRecommended(1000)
                .saveAsync(senderPersister)
                .get();
        try (SenderReplayResult result = Payjoin.replaySenderEventLogAsync(senderPersister).get()) {
            assertInstanceOf(SendSession.WithReplyKey.class, result.state());
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
