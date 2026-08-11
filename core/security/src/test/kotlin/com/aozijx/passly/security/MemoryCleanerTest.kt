package com.aozijx.passly.security

import com.aozijx.passly.security.crypto.SecureString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `secure string returns copies and wipes its owned value`() {
        val value = SecureString.fromString("secret")
        val exported = value.toCharArray()

        exported.fill('x')
        assertEquals("secret", String(value.toCharArray()))

        value.wipe()
        assertTrue(value.isWiped)
        assertArrayEquals(CharArray(6), value.toCharArray())
    }
}
