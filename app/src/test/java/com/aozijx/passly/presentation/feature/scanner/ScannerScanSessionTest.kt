package com.aozijx.passly.presentation.feature.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerScanSessionTest {

    @Test
    fun `duplicate value is rejected until session resets`() {
        val session = ScannerScanSession()

        assertTrue(session.accept("same-value"))
        assertFalse(session.accept("same-value"))

        session.reset()

        assertTrue(session.accept("same-value"))
    }

    @Test
    fun `blank values never enter the session`() {
        val session = ScannerScanSession()

        assertFalse(session.accept("  "))
        assertTrue(session.accept("next-value"))
    }
}
