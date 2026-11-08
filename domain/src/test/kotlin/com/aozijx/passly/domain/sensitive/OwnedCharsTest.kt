package com.aozijx.passly.domain.sensitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnedCharsTest {
    @Test
    fun `toCharArray returns a copy and wipe clears the owned value`() {
        val value = OwnedChars.fromString("secret")
        val exported = value.toCharArray()

        exported.fill('x')
        assertEquals("secret", String(value.toCharArray()))

        value.wipe()
        assertTrue(value.isWiped)
        assertTrue(value.toCharArray().isEmpty())
    }
}
