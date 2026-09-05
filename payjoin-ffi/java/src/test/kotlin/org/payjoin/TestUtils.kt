package org.payjoin

/** `_test-utils` helpers. Not part of the production `org.payjoin` API. */
object TestUtils {
    @JvmStatic
    fun exampleUrl(): String = org.payjoindevkit.exampleUrl()

    @JvmStatic
    fun originalPsbt(): String = org.payjoindevkit.originalPsbt()
}
