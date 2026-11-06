package com.aozijx.passly.feature.scanner.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerReducerTest {
    @Test
    fun `starting a scan clears the previous result`() {
        val result =
            ScannerReducer.reduce(
                ScannerUiState.ShowingResult(ScannerResult("old result")),
                ScannerMutation.Started,
            )

        assertEquals(ScannerUiState.Scanning, result)
    }

    @Test
    fun `completed scan owns one raw result and stops analysis`() {
        val scanResult =
            ScannerResult(rawValue = "otpauth://totp/example")
        val result =
            ScannerReducer.reduce(
                ScannerUiState.Scanning,
                ScannerMutation.ScanCompleted(scanResult),
            )

        assertEquals(ScannerUiState.ShowingResult(scanResult), result)
    }

    @Test
    fun `stopping scanner clears sensitive result state`() {
        val result =
            ScannerReducer.reduce(
                ScannerUiState.ShowingResult(
                    ScannerResult(rawValue = "otpauth://totp/example?secret=secret"),
                ),
                ScannerMutation.Stopped,
            )

        assertEquals(ScannerUiState.Stopped, result)
    }

    @Test
    fun `image processing is an explicit phase`() {
        val result =
            ScannerReducer.reduce(
                ScannerUiState.Scanning,
                ScannerMutation.ImageProcessingStarted,
            )

        assertEquals(ScannerUiState.ProcessingImage, result)
    }

    @Test
    fun `late image result cannot revive a stopped scanner`() {
        val lateResult = ScannerResult("late")

        assertEquals(
            ScannerUiState.Stopped,
            ScannerReducer.reduce(
                ScannerUiState.Stopped,
                ScannerMutation.ScanCompleted(lateResult),
            ),
        )
    }
}
