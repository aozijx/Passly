package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerReducerTest {

    @Test
    fun `starting a scan clears the previous typed result`() {
        val result = ScannerReducer.reduce(
            ScannerUiState.ShowingResult(ScannerResult.PlainText("old result")),
            ScannerMutation.Started,
        )

        assertEquals(ScannerUiState.Scanning, result)
    }

    @Test
    fun `completed scan owns one typed result and stops analysis`() {
        val scanResult = ScannerResult.Otp(
            rawValue = "otpauth://totp/example",
            config = OtpConfig(secret = "secret"),
        )
        val result = ScannerReducer.reduce(
            ScannerUiState.Scanning,
            ScannerMutation.ScanCompleted(scanResult),
        )

        assertEquals(ScannerUiState.ShowingResult(scanResult), result)
    }

    @Test
    fun `stopping scanner clears sensitive result state`() {
        val result = ScannerReducer.reduce(
            ScannerUiState.ShowingResult(
                ScannerResult.Otp(
                    rawValue = "otpauth://totp/example?secret=secret",
                    config = OtpConfig(secret = "secret"),
                ),
            ),
            ScannerMutation.Stopped,
        )

        assertEquals(ScannerUiState.Stopped, result)
    }

    @Test
    fun `image processing is an explicit phase`() {
        val result = ScannerReducer.reduce(
            ScannerUiState.Scanning,
            ScannerMutation.ImageProcessingStarted,
        )

        assertEquals(ScannerUiState.ProcessingImage, result)
    }

    @Test
    fun `late image result cannot revive a stopped scanner`() {
        val lateResult = ScannerResult.PlainText("late")

        assertEquals(
            ScannerUiState.Stopped,
            ScannerReducer.reduce(
                ScannerUiState.Stopped,
                ScannerMutation.ScanCompleted(lateResult),
            ),
        )
    }
}
