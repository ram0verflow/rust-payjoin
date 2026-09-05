package org.payjoin

import org.payjoindevkit.OhttpKeys as FfiOhttpKeys
import org.payjoindevkit.PjUri as FfiPjUri
import org.payjoindevkit.Uri as FfiUri
import org.payjoindevkit.Url as FfiUrl

/** A BIP21 `bitcoin:` URI. */
class Uri internal constructor(internal val inner: FfiUri) : AutoCloseable {
    fun address(): String = inner.address()

    fun amountSats(): Long? = inner.amountSats()?.toLong()

    fun asString(): String = inner.asString()

    fun label(): String? = inner.label()

    fun message(): String? = inner.message()

    @Throws(PjNotSupportedException::class)
    fun checkPjSupported(): PjUri =
        try {
            PjUri(inner.checkPjSupported())
        } catch (e: org.payjoindevkit.PjNotSupported) {
            throw PjNotSupportedException(e.message, e)
        }

    override fun close() {
        inner.close()
    }

    companion object {
        @JvmStatic
        @Throws(UriParseException::class)
        fun parse(uri: String): Uri = wrapUriParse { Uri(FfiUri.parse(uri)) }
    }
}

/** A URL, including payjoin endpoint URLs embedded in BIP21 URIs. */
class Url internal constructor(internal val inner: FfiUrl) : AutoCloseable {
    fun asString(): String = inner.asString()

    fun query(): String? = inner.query()

    override fun close() {
        inner.close()
    }

    companion object {
        @JvmStatic
        @Throws(UrlParseException::class)
        fun parse(input: String): Url = wrapUrlParse { Url(FfiUrl.parse(input)) }
    }
}

/** A BIP21 URI that includes a payjoin endpoint. */
class PjUri internal constructor(internal val inner: FfiPjUri) : AutoCloseable {
    fun address(): String = inner.address()

    fun amountSats(): Long? = inner.amountSats()?.toLong()

    fun asString(): String = inner.asString()

    fun pjEndpoint(): String = inner.pjEndpoint()

    fun setAmountSats(amountSats: Long): PjUri = PjUri(inner.setAmountSats(u64("amountSats", amountSats)))

    override fun close() {
        inner.close()
    }
}

/** Oblivious HTTP keys used by BIP77. */
class OhttpKeys internal constructor(internal val inner: FfiOhttpKeys) : AutoCloseable {
    override fun close() {
        inner.close()
    }

    companion object {
        @JvmStatic
        @Throws(OhttpException::class)
        fun decode(bytes: ByteArray): OhttpKeys = wrapOhttp { OhttpKeys(FfiOhttpKeys.decode(bytes)) }
    }
}
