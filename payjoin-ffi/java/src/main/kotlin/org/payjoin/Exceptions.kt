package org.payjoin

/**
 * Thrown when a bitcoin: URI cannot be parsed.
 */
class UriParseException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when a URL cannot be parsed. */
class UrlParseException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when OHTTP keys cannot be decoded. */
class OhttpException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when [ReceiverBuilder] construction or configuration fails. */
class ReceiverBuilderException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when [SenderBuilder] is given an invalid PSBT or URI. */
class SenderInputException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when receiver session replay fails. */
class ReceiverReplayException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when sender session replay fails. */
class SenderReplayException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

/** Thrown when a URI has no payjoin endpoint. */
class PjNotSupportedException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

internal inline fun <T> wrapUriParse(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.UriParseException) {
        throw UriParseException(e.message, e)
    }

internal inline fun <T> wrapUrlParse(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.UrlParseException) {
        throw UrlParseException(e.message, e)
    }

internal inline fun <T> wrapOhttp(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.OhttpException) {
        throw OhttpException(e.message, e)
    }

internal inline fun <T> wrapReceiverBuilder(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.ReceiverBuilderException) {
        throw ReceiverBuilderException(e.message, e)
    }

internal inline fun <T> wrapSenderInput(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.SenderInputException) {
        throw SenderInputException(e.message, e)
    }

internal inline fun <T> wrapReceiverReplay(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.ReceiverReplayException) {
        throw ReceiverReplayException(e.message, e)
    }

internal inline fun <T> wrapSenderReplay(block: () -> T): T =
    try {
        block()
    } catch (e: org.payjoindevkit.SenderReplayException) {
        throw SenderReplayException(e.message, e)
    }
