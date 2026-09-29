package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.presentation.feature.scanner.ScannerUiState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerReducerTest {

    @Test
    fun `starting a scan clears the previous result and error`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(
                isScanning = false,
                scanResult = "old result",
                scannedOtp = OtpConfig(secret = "secret"),
            ),
            ScannerMutation.Started,
        )

        assertTrue(result.isScanning)
        assertEquals("", result.scanResult)
        assertNull(result.scannedOtp)
    }

    @Test
    fun `successful scan owns the decoded result in state`() {
        val otp = OtpConfig(secret = "secret")
        val result = ScannerReducer.reduce(
            ScannerUiState(),
            ScannerMutation.ScanCompleted(
                result = "otpauth://totp/example",
                otpConfig = otp,
            ),
        )

        assertFalse(result.isScanning)
        assertEquals("otpauth://totp/example", result.scanResult)
        assertEquals(otp, result.scannedOtp)
    }

    @Test
    fun `stopping scanner clears sensitive result state`() {
        val result = ScannerReducer.reduce(
            ScannerUiState(
                scanResult = "otpauth://totp/example?secret=secret",
                scannedOtp = OtpConfig(secret = "secret"),
            ),
            ScannerMutation.Stopped,
        )

        assertFalse(result.isScanning)
        assertEquals("", result.scanResult)
        assertNull(result.scannedOtp)
    }
}
