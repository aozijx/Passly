package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.ByteArrayOutputStream

/** Byte buffer for backup JSON and records that must be erased on release. */
internal class WipingByteArrayOutputStream(
    initialSize: Int = 32,
) : ByteArrayOutputStream(initialSize) {
    fun wipe() {
        buf.fill(0)
        reset()
    }
}
