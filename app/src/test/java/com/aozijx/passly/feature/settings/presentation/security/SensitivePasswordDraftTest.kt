package com.aozijx.passly.feature.settings.presentation.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivePasswordDraftTest {
    @Test
    fun `replace zeros previously owned array and clear zeros current array`() {
        val draft = SensitivePasswordDraft()
        val first = "first-password".toCharArray()
        val second = "second-password".toCharArray()
        draft.replace(first)

        assertEquals("first-password", draft.snapshot())
        draft.replace(second)

        assertTrue(first.all { it == '\u0000' })
        assertEquals("second-password", draft.snapshot())
        draft.clear()

        assertTrue(second.all { it == '\u0000' })
        assertEquals("", draft.snapshot())
    }

    @Test
    fun `copy returns independently owned contents`() {
        val draft = SensitivePasswordDraft()
        val owned = "private-password".toCharArray()
        draft.replace(owned)

        val copy = draft.copy()
        draft.clear()

        assertArrayEquals("private-password".toCharArray(), copy)
        assertTrue(owned.all { it == '\u0000' })
        copy.fill('\u0000')
    }
}
