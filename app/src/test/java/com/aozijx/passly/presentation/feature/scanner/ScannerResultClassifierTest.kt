package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerResultClassifierTest {

    @Test
    fun `blank values do not produce a result`() {
        listOf("", "  ", "\n\t").forEach { rawValue ->
            assertEquals(
                ScannerClassification.Empty,
                ScannerResultClassifier.classify(rawValue, noOtpParser),
            )
        }
    }

    @Test
    fun `valid otp uri produces otp result with parsed config`() {
        val rawValue = "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP"
        val config = OtpConfig(
            secret = "JBSWY3DPEHPK3PXP",
            issuer = "Example",
            accountName = "alice",
        )

        val result = classified(rawValue) { candidate ->
            config.takeIf { candidate == rawValue }
        }

        assertTrue(result is ScannerResult.Otp)
        result as ScannerResult.Otp
        assertEquals(rawValue, result.rawValue)
        assertEquals(config, result.config)
    }

    @Test
    fun `http and https values produce web link results`() {
        mapOf(
            "https://example.com/path?q=1" to URI("https://example.com/path?q=1"),
            "http://sub.example.com" to URI("http://sub.example.com"),
        ).forEach { (rawValue, expectedUri) ->
            val result = classified(rawValue, noOtpParser)

            assertTrue(result is ScannerResult.WebLink)
            result as ScannerResult.WebLink
            assertEquals(rawValue, result.rawValue)
            assertEquals(expectedUri.scheme, result.uri.scheme)
            assertEquals(expectedUri.host, result.uri.host)
        }
    }

    @Test
    fun `valid wifi produces a typed wifi result`() {
        val rawValue = "WIFI:S:Office;T:WPA;P:password;H:true;;"

        val result = classified(rawValue, noOtpParser)

        assertTrue(result is ScannerResult.Wifi)
        result as ScannerResult.Wifi
        assertEquals(rawValue, result.rawValue)
        assertEquals("Office", result.payload.ssid)
        assertEquals("password", result.payload.password)
        assertEquals(WifiSecurityType.WpaPersonal, result.payload.securityType)
        assertEquals(true, result.payload.isHidden)
    }

    @Test
    fun `malformed and unsupported wifi remain plain text`() {
        listOf(
            "WIFI:S:Office;T:vendor-private;P:password;;",
            "WIFI:S:Office;T:WPA;E:TTLS;P:password;;",
            "WIFI:T:WPA;P:password;;",
        ).forEach { rawValue ->
            assertEquals(ScannerResult.PlainText(rawValue), classified(rawValue, noOtpParser))
        }
    }

    @Test
    fun `unsupported schemes produce plain text results`() {
        val rawValue = "mailto:ada@example.com"

        assertEquals(ScannerResult.PlainText(rawValue), classified(rawValue, noOtpParser))
    }

    @Test
    fun `malformed web links produce plain text instead of throwing`() {
        listOf("https://", "https://exa mple.com").forEach { rawValue ->
            val result = classified(rawValue, noOtpParser)

            assertTrue(result is ScannerResult.PlainText)
            assertEquals(rawValue, result.rawValue)
        }
    }

    @Test
    fun `malformed otp uri remains available as plain text`() {
        val rawValue = "otpauth://totp/Example?issuer=Example"

        val result = classified(rawValue, noOtpParser)

        assertTrue(result is ScannerResult.PlainText)
        assertEquals(rawValue, result.rawValue)
    }

    private fun classified(
        rawValue: String,
        otpParser: (String) -> OtpConfig? = noOtpParser,
    ): ScannerResult = when (val result = ScannerResultClassifier.classify(rawValue, otpParser)) {
        is ScannerClassification.Classified -> result.result
        ScannerClassification.Empty -> error("Expected a classified scanner result")
    }

    private companion object {
        val noOtpParser: (String) -> OtpConfig? = { null }
    }
}
