package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiQrPayloadFormatterTest {
    @Test
    fun `personal network escapes separators and preserves hidden flag`() {
        val result = WifiQrPayloadFormatter.format(
            WifiQrPayload("Office;West", "pa\\ss:word", WifiSecurityType.WpaPersonal, true),
        )

        assertEquals(
            WifiQrFormatResult.Formatted("""WIFI:T:WPA;S:Office\;West;P:pa\\ss\:word;H:true;;"""),
            result,
        )
    }

    @Test
    fun `open network never exports a leftover password`() {
        assertEquals(
            WifiQrFormatResult.Formatted("WIFI:T:nopass;S:Guest;H:false;;"),
            WifiQrPayloadFormatter.format(
                WifiQrPayload("Guest", "leftover-secret", WifiSecurityType.Open, false),
            ),
        )
    }

    @Test
    fun `wep uses its stable protocol key`() {
        assertEquals(
            WifiQrFormatResult.Formatted("WIFI:T:WEP;S:Legacy;P:abcde;H:false;;"),
            WifiQrPayloadFormatter.format(
                WifiQrPayload("Legacy", "abcde", WifiSecurityType.Wep, false),
            ),
        )
    }

    @Test
    fun `special characters unicode and field-like input round trip`() {
        val payload = WifiQrPayload(
            ssid = "办公室\\\";T:nopass;,:",
            password = "pa\\\";ss,word:",
            securityType = WifiSecurityType.WpaPersonal,
            isHidden = true,
        )
        val result = WifiQrPayloadFormatter.format(payload)
        assertTrue(result is WifiQrFormatResult.Formatted)

        assertEquals(
            WifiQrParseResult.Parsed(payload),
            WifiQrPayloadParser.parse((result as WifiQrFormatResult.Formatted).content),
        )
    }

    @Test
    fun `incomplete credentials cannot produce a connection code`() {
        listOf(
            WifiQrPayload(" ", "password", WifiSecurityType.WpaPersonal, false),
            WifiQrPayload("Office", "", WifiSecurityType.WpaPersonal, false),
            WifiQrPayload("Office", "short", WifiSecurityType.WpaPersonal, false),
            WifiQrPayload("Office", "abcd", WifiSecurityType.Wep, false),
        ).forEach { payload ->
            assertEquals(WifiQrFormatResult.InvalidCredential, WifiQrPayloadFormatter.format(payload))
        }
    }
}
