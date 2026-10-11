package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerPurposeTest {
    private val otpConfig = OtpConfig(secret = "secret")
    private val wifiPayload = WifiQrPayload(
        ssid = "Office",
        password = "password",
        securityType = WifiSecurityType.WpaPersonal,
        isHidden = false,
    )

    @Test
    fun `general scanner exposes both typed imports`() {
        assertEquals(
            ScannerImportResolution.Available(ScannerImport.Otp(otpConfig)),
            ScannerPurpose.General.resolveImport(ScannerResult.Otp("otp", otpConfig)),
        )
        assertEquals(
            ScannerImportResolution.Available(ScannerImport.Wifi(wifiPayload)),
            ScannerPurpose.General.resolveImport(ScannerResult.Wifi("wifi", wifiPayload)),
        )
    }

    @Test
    fun `editor scanner only exposes its matching import`() {
        assertEquals(
            ScannerImportResolution.Available(ScannerImport.Otp(otpConfig)),
            ScannerPurpose.ApplyToOtpEditor.resolveImport(ScannerResult.Otp("otp", otpConfig)),
        )
        assertEquals(
            ScannerImportResolution.Unavailable,
            ScannerPurpose.ApplyToOtpEditor.resolveImport(ScannerResult.Wifi("wifi", wifiPayload)),
        )
        assertEquals(
            ScannerImportResolution.Available(ScannerImport.Wifi(wifiPayload)),
            ScannerPurpose.ApplyToWifiEditor.resolveImport(ScannerResult.Wifi("wifi", wifiPayload)),
        )
        assertEquals(
            ScannerImportResolution.Unavailable,
            ScannerPurpose.ApplyToWifiEditor.resolveImport(ScannerResult.Otp("otp", otpConfig)),
        )
    }

    @Test
    fun `non import results are explicitly unavailable`() {
        listOf(
            ScannerResult.WebLink("https://example.com", URI("https://example.com")),
            ScannerResult.PlainText("text"),
        ).forEach { result ->
            ScannerPurpose.entries.forEach { purpose ->
                assertEquals(
                    ScannerImportResolution.Unavailable,
                    purpose.resolveImport(result),
                )
            }
        }
    }
}
