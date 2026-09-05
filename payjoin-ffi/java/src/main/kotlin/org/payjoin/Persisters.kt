package org.payjoin

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.payjoin.internal.asyncScope

/**
 * Session persister that saves and loads events as JSON strings.
 *
 * Java code implements this interface. Do not implement the generated
 * `org.payjoindevkit.JsonReceiverSessionPersister`.
 */
interface JsonReceiverSessionPersister {
    fun save(event: String)

    fun load(): List<String>

    /** Protocol teardown. Not [AutoCloseable.close], which drops the Rust handle. */
    fun closeSession()
}

/** Async receiver persister. Methods return [CompletableFuture] instead of Kotlin `suspend`. */
interface JsonReceiverSessionPersisterAsync {
    fun save(event: String): CompletableFuture<Void>

    fun load(): CompletableFuture<List<String>>

    fun closeSession(): CompletableFuture<Void>
}

/** Session persister that saves and loads sender events as JSON strings. */
interface JsonSenderSessionPersister {
    fun save(event: String)

    fun load(): List<String>

    fun closeSession()
}

/** Async sender persister using [CompletableFuture]. */
interface JsonSenderSessionPersisterAsync {
    fun save(event: String): CompletableFuture<Void>

    fun load(): CompletableFuture<List<String>>

    fun closeSession(): CompletableFuture<Void>
}

internal fun JsonReceiverSessionPersister.asUniFfi(): org.payjoindevkit.JsonReceiverSessionPersister =
    JavaReceiverPersisterAdapter(this)

internal fun JsonSenderSessionPersister.asUniFfi(): org.payjoindevkit.JsonSenderSessionPersister =
    JavaSenderPersisterAdapter(this)

internal fun JsonReceiverSessionPersisterAsync.asUniFfi(): org.payjoindevkit.JsonReceiverSessionPersisterAsync =
    JavaReceiverPersisterAsyncAdapter(this)

internal fun JsonSenderSessionPersisterAsync.asUniFfi(): org.payjoindevkit.JsonSenderSessionPersisterAsync =
    JavaSenderPersisterAsyncAdapter(this)

private class JavaReceiverPersisterAdapter(
    private val java: JsonReceiverSessionPersister,
) : org.payjoindevkit.JsonReceiverSessionPersister {
    override fun save(event: String) = java.save(event)

    override fun load(): List<String> = java.load()

    override fun closeSession() = java.closeSession()
}

private class JavaSenderPersisterAdapter(
    private val java: JsonSenderSessionPersister,
) : org.payjoindevkit.JsonSenderSessionPersister {
    override fun save(event: String) = java.save(event)

    override fun load(): List<String> = java.load()

    override fun closeSession() = java.closeSession()
}

private class JavaReceiverPersisterAsyncAdapter(
    private val java: JsonReceiverSessionPersisterAsync,
) : org.payjoindevkit.JsonReceiverSessionPersisterAsync {
    override suspend fun save(event: String) {
        java.save(event).await()
    }

    override suspend fun load(): List<String> = java.load().await()

    override suspend fun closeSession() {
        java.closeSession().await()
    }
}

private class JavaSenderPersisterAsyncAdapter(
    private val java: JsonSenderSessionPersisterAsync,
) : org.payjoindevkit.JsonSenderSessionPersisterAsync {
    override suspend fun save(event: String) {
        java.save(event).await()
    }

    override suspend fun load(): List<String> = java.load().await()

    override suspend fun closeSession() {
        java.closeSession().await()
    }
}

internal fun <T> runAsync(block: suspend () -> T): CompletableFuture<T> =
    asyncScope.future { block() }
