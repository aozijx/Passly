package com.aozijx.passly.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MemoryCleanerTest {
    @Test
    fun `arrays are overwritten in place`() {
        val bytes = byteArrayOf(1, 2, 3)
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')

        MemoryCleaner.wipeByteArray(bytes)
        MemoryCleaner.wipeCharArray(chars)

        assertArrayEquals(byteArrayOf(0, 0, 0), bytes)
        assertArrayEquals(CharArray(6), chars)
    }
}
