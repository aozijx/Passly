package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LimitedInputStreamTest {
    @Test
    fun zeroLengthReadAtLimitDoesNotConsumeOrRejectTheNextByte() {
        val input = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2)), maxBytes = 1)
        assertEquals(1, input.read())

        assertEquals(0, input.read(ByteArray(1), 0, 0))
        assertThrows(IllegalStateException::class.java) { input.read() }
    }
}
