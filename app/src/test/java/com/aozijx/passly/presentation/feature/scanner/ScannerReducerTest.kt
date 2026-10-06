package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerReducerTest {

    @Test
    fun `starting a scan clears the previous typed result`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(
                isScanning = false,
                result = ScannerResult.PlainText("old result"),
            ),
            ScannerMutation.Started,
        )

        assertTrue(result.isScanning)
        assertNull(result.result)
    }

    @Test
    fun `completed scan owns one typed result and stops analysis`() {
        val scanResult = ScannerResult.Otp(
            rawValue = "otpauth://totp/example",
            config = OtpConfig(secret = "secret"),
        )
        val result = ScannerReducer.reduce(
            ScannerUiState(),
            ScannerMutation.ScanCompleted(scanResult),
        )

        assertFalse(result.isScanning)
        assertEquals(scanResult, result.result)
    }

    @Test
    fun `stopping scanner clears sensitive result state`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(
                result = ScannerResult.Otp(
                    rawValue = "otpauth://totp/example?secret=secret",
                    config = OtpConfig(secret = "secret"),
                ),
            ),
            ScannerMutation.Stopped,
        )

        assertFalse(result.isScanning)
        assertNull(result.result)
    }
}
