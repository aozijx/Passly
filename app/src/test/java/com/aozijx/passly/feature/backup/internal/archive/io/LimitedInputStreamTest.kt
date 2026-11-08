package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LimitedInputStreamTest {
    @Test
    fun bulkReadsRespectLimitAndZeroLengthReadsDoNotConsumeTheNextByte() {
        val exact = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), maxBytes = 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), exact.readBytes())

        val oversized = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), maxBytes = 3)
        assertThrows(IllegalStateException::class.java) { oversized.readBytes() }

        val input = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2)), maxBytes = 1)
        assertEquals(1, input.read())

        assertEquals(0, input.read(ByteArray(1), 0, 0))
        assertThrows(IllegalStateException::class.java) { input.read() }
    }
}
