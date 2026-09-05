package org.payjoindevkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PersistenceTests {
    @Test
    void receiverPersistence() throws Exception {
        InMemoryReceiverPersister persister = new InMemoryReceiverPersister();
        try (OhttpKeys ohttpKeys = OhttpKeys.decode(TestFixtures.OHTTP_KEYS);
                ReceiverBuilder builder = new ReceiverBuilder(
                    "tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4",
                    "https://example.com",
                    ohttpKeys);
                InitialReceiveTransition transition = builder.build();
                Initialized ignored = transition.save(persister);
                ReplayResult replay = Payjoin.replayReceiverEventLog(persister);
                ReceiveSession state = replay.state()) {
            assertInstanceOf(ReceiveSession.Initialized.class, state);
        }
        assertFalse(persister.isClosed());
        persister.closeSession();
        assertTrue(persister.isClosed());
    }

    @Test
    void senderPersistence() throws Exception {
        InMemoryReceiverPersister receiverPersister = new InMemoryReceiverPersister();
        InMemorySenderPersister senderPersister = new InMemorySenderPersister();
        try (OhttpKeys ohttpKeys = OhttpKeys.decode(TestFixtures.OHTTP_KEYS);
                ReceiverBuilder builder = new ReceiverBuilder(
                    "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK",
                    "https://example.com",
                    ohttpKeys);
                InitialReceiveTransition receiveTransition = builder.build();
                Initialized receiver = receiveTransition.save(receiverPersister);
                PjUri uri = receiver.pjUri();
                SenderBuilder senderBuilder = new SenderBuilder(Payjoin.originalPsbt(), uri);
                InitialSendTransition sendTransition = senderBuilder.buildRecommended(1000L);
                WithReplyKey ignored = sendTransition.save(senderPersister);
                SenderReplayResult replay = Payjoin.replaySenderEventLog(senderPersister);
                SendSession state = replay.state()) {
            assertInstanceOf(SendSession.WithReplyKey.class, state);
        }
        receiverPersister.closeSession();
        senderPersister.closeSession();
        assertTrue(receiverPersister.isClosed());
        assertTrue(senderPersister.isClosed());
    }

    @Test
    void receiverPersistenceAsync() throws Exception {
        InMemoryReceiverPersisterAsync persister = new InMemoryReceiverPersisterAsync();
        try (OhttpKeys ohttpKeys = OhttpKeys.decode(TestFixtures.OHTTP_KEYS);
                ReceiverBuilder builder = new ReceiverBuilder(
                    "tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4",
                    "https://example.com",
                    ohttpKeys);
                InitialReceiveTransition transition = builder.build();
                Initialized ignored = transition.saveAsync(persister).join();
                ReplayResult replay = Payjoin.replayReceiverEventLogAsync(persister).join();
                ReceiveSession state = replay.state()) {
            assertInstanceOf(ReceiveSession.Initialized.class, state);
        }
        persister.closeSession().join();
        assertTrue(persister.isClosed());
    }

    @Test
    void senderPersistenceAsync() throws Exception {
        InMemoryReceiverPersisterAsync receiverPersister = new InMemoryReceiverPersisterAsync();
        InMemorySenderPersisterAsync senderPersister = new InMemorySenderPersisterAsync();
        try (OhttpKeys ohttpKeys = OhttpKeys.decode(TestFixtures.OHTTP_KEYS);
                ReceiverBuilder builder = new ReceiverBuilder(
                    "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK",
                    "https://example.com",
                    ohttpKeys);
                InitialReceiveTransition receiveTransition = builder.build();
                Initialized receiver = receiveTransition.saveAsync(receiverPersister).join();
                PjUri uri = receiver.pjUri();
                SenderBuilder senderBuilder = new SenderBuilder(Payjoin.originalPsbt(), uri);
                InitialSendTransition sendTransition = senderBuilder.buildRecommended(1000L);
                WithReplyKey ignored = sendTransition.saveAsync(senderPersister).join();
                SenderReplayResult replay = Payjoin.replaySenderEventLogAsync(senderPersister).join();
                SendSession state = replay.state()) {
            assertInstanceOf(SendSession.WithReplyKey.class, state);
        }
        receiverPersister.closeSession().join();
        senderPersister.closeSession().join();
        assertTrue(receiverPersister.isClosed());
        assertTrue(senderPersister.isClosed());
    }
}
