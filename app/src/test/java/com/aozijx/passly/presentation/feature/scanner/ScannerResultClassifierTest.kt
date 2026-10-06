package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerResultClassifierTest {

    @Test
    fun `blank values do not produce a result`() {
        listOf("", "  ", "\n\t").forEach { rawValue ->
            assertNull(ScannerResultClassifier.classify(rawValue, noOtpParser))
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

        val result = ScannerResultClassifier.classify(rawValue) { candidate ->
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
            val result = ScannerResultClassifier.classify(rawValue, noOtpParser)

            assertTrue(result is ScannerResult.WebLink)
            result as ScannerResult.WebLink
            assertEquals(rawValue, result.rawValue)
            assertEquals(expectedUri.scheme, result.uri.scheme)
            assertEquals(expectedUri.host, result.uri.host)
        }
    }

    @Test
    fun `unsupported schemes produce plain text results`() {
        val rawValue = "wifi:S:Office;T:WPA;P:secret;;"

        val result = ScannerResultClassifier.classify(rawValue, noOtpParser)

        assertTrue(result is ScannerResult.PlainText)
        assertEquals(rawValue, result?.rawValue)
    }

    @Test
    fun `malformed web links produce plain text instead of throwing`() {
        listOf("https://", "https://exa mple.com").forEach { rawValue ->
            val result = ScannerResultClassifier.classify(rawValue, noOtpParser)

            assertTrue(result is ScannerResult.PlainText)
            assertEquals(rawValue, result?.rawValue)
        }
    }

    @Test
    fun `malformed otp uri remains available as plain text`() {
        val rawValue = "otpauth://totp/Example?issuer=Example"

        val result = ScannerResultClassifier.classify(rawValue, noOtpParser)

        assertTrue(result is ScannerResult.PlainText)
        assertEquals(rawValue, result?.rawValue)
    }

    private companion object {
        val noOtpParser: (String) -> OtpConfig? = { null }
    }
}
