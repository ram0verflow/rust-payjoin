package org.payjoin

/** Java `long`/`int` are signed; UniFFI wants `u64`/`u8`. Do not bit-cast. */
internal fun u64(name: String, value: Long): ULong {
    require(value >= 0) { "$name must be non-negative, got $value" }
    return value.toULong()
}

internal fun u8(name: String, value: Int): UByte {
    require(value in 0..255) { "$name must be in 0..255, got $value" }
    return value.toUByte()
}
