package org.payjoindevkit;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

abstract class MemoryEventLog {
    private final CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    void saveEvent(String event) {
        events.add(event);
    }

    List<String> loadEvents() {
        return List.copyOf(events);
    }

    void markClosed() {
        closed = true;
    }

    boolean isClosed() {
        return closed;
    }
}

final class InMemoryReceiverPersister extends MemoryEventLog implements JsonReceiverSessionPersister {
    @Override
    public void save(String event) {
        saveEvent(event);
    }

    @Override
    public List<String> load() {
        return loadEvents();
    }

    @Override
    public void closeSession() {
        markClosed();
    }
}

final class InMemorySenderPersister extends MemoryEventLog implements JsonSenderSessionPersister {
    @Override
    public void save(String event) {
        saveEvent(event);
    }

    @Override
    public List<String> load() {
        return loadEvents();
    }

    @Override
    public void closeSession() {
        markClosed();
    }
}

final class InMemoryReceiverPersisterAsync extends MemoryEventLog implements JsonReceiverSessionPersisterAsync {
    @Override
    public CompletableFuture<Void> save(String event) {
        saveEvent(event);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<String>> load() {
        return CompletableFuture.completedFuture(loadEvents());
    }

    @Override
    public CompletableFuture<Void> closeSession() {
        markClosed();
        return CompletableFuture.completedFuture(null);
    }
}

final class InMemorySenderPersisterAsync extends MemoryEventLog implements JsonSenderSessionPersisterAsync {
    @Override
    public CompletableFuture<Void> save(String event) {
        saveEvent(event);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<String>> load() {
        return CompletableFuture.completedFuture(loadEvents());
    }

    @Override
    public CompletableFuture<Void> closeSession() {
        markClosed();
        return CompletableFuture.completedFuture(null);
    }
}
