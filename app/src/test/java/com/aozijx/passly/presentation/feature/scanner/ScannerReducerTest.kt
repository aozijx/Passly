package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.presentation.feature.scanner.ScannerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerReducerTest {

    @Test
    fun `starting a scan resets the previous error`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(isScanning = false, error = "old"),
            ScannerMutation.Started,
        )

        assertTrue(result.isScanning)
        assertNull(result.error)
    }

    @Test
    fun `successful scan stops and clears stale error`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(error = "old"),
            ScannerMutation.ScanCompleted,
        )

        assertFalse(result.isScanning)
        assertNull(result.error)
    }

    @Test
    fun `decode failure keeps scanner available for another image`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(),
            ScannerMutation.DecodeFailed("decode failed"),
        )

        assertTrue(result.isScanning)
        assertEquals("decode failed", result.error)
    }
}
