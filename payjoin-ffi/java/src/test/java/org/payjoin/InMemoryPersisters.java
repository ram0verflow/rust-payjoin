package org.payjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

abstract class MemoryEventLog {
    private final List<String> events = new ArrayList<>();
    private boolean closed;

    public void save(String event) {
        events.add(event);
    }

    public List<String> load() {
        return List.copyOf(events);
    }

    public void closeSession() {
        closed = true;
    }

    boolean isClosed() {
        return closed;
    }
}

abstract class MemoryEventLogAsync {
    private final List<String> events = new ArrayList<>();
    private boolean closed;

    public CompletableFuture<Void> save(String event) {
        events.add(event);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<String>> load() {
        return CompletableFuture.completedFuture(List.copyOf(events));
    }

    public CompletableFuture<Void> closeSession() {
        closed = true;
        return CompletableFuture.completedFuture(null);
    }

    boolean isClosed() {
        return closed;
    }
}

final class InMemoryReceiverPersister extends MemoryEventLog implements JsonReceiverSessionPersister {}

final class InMemorySenderPersister extends MemoryEventLog implements JsonSenderSessionPersister {}

final class InMemoryReceiverPersisterAsync extends MemoryEventLogAsync
        implements JsonReceiverSessionPersisterAsync {}

final class InMemorySenderPersisterAsync extends MemoryEventLogAsync
        implements JsonSenderSessionPersisterAsync {}
