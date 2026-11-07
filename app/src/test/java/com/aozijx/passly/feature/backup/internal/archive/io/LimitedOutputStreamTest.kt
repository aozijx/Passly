package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LimitedOutputStreamTest {
    @Test
    fun write_acceptsExactLimit() {
        val destination = ByteArrayOutputStream()

        LimitedOutputStream(destination, 3).use { it.write(byteArrayOf(1, 2, 3)) }

        assertArrayEquals(byteArrayOf(1, 2, 3), destination.toByteArray())
    }

    @Test
    fun write_rejectsOverLimitBeforeWritingAnyOfTheRejectedBuffer() {
        val destination = ByteArrayOutputStream()
        val limited = LimitedOutputStream(destination, 3)
        limited.write(byteArrayOf(1, 2))

        assertThrows(IllegalStateException::class.java) {
            limited.write(byteArrayOf(3, 4))
        }
        assertArrayEquals(byteArrayOf(1, 2), destination.toByteArray())
    }
}
