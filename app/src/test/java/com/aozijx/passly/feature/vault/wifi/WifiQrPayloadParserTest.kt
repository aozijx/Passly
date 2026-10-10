package com.aozijx.passly.feature.vault.wifi

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiQrPayloadParserTest {
    @Test
    fun `field order and supported security aliases do not affect the payload`() {
        mapOf(
            "WIFI:P:password;S:Office;T:WEP;H:false;;" to WifiSecurityType.Wep,
            "wifi:H:TRUE;T:wpa;S:Office;P:password;;" to WifiSecurityType.WpaPersonal,
            "WIFI:T:WPA2;P:password;S:Office;;" to WifiSecurityType.WpaPersonal,
            "WIFI:S:Office;T:WPA3;P:password;;" to WifiSecurityType.WpaPersonal,
            "WIFI:T:SAE;S:Office;P:password;;" to WifiSecurityType.WpaPersonal,
        ).forEach { (rawValue, expectedSecurity) ->
            val payload = requireNotNull(WifiQrPayloadParser.parse(rawValue))

            assertEquals("Office", payload.ssid)
            assertEquals("password", payload.password)
            assertEquals(expectedSecurity, payload.securityType)
        }
    }

    @Test
    fun `missing or open security clears any supplied password`() {
        listOf(
            "WIFI:S:Guest;P:ignored;;",
            "WIFI:T:nopass;S:Guest;P:ignored;;",
            "WIFI:S:Guest;T:OPEN;P:ignored;;",
        ).forEach { rawValue ->
            val payload = requireNotNull(WifiQrPayloadParser.parse(rawValue))

            assertEquals(WifiSecurityType.Open, payload.securityType)
            assertEquals("", payload.password)
            assertEquals(false, payload.isHidden)
        }
    }

    @Test
    fun `escaped delimiters and backslash are restored exactly once`() {
        val payload = requireNotNull(
            WifiQrPayloadParser.parse(
                "WIFI:P:p\\;a\\\\s\\,s\\\"w\\:d;H:TrUe;S:Office\\; East;T:WPA;;",
            ),
        )

        assertEquals("Office; East", payload.ssid)
        assertEquals("p;a\\s,s\"w:d", payload.password)
        assertEquals(true, payload.isHidden)
    }

    @Test
    fun `malformed or unsupported wifi values never expose a partial payload`() {
        listOf(
            "WIFI:T:WPA;P:password;;",
            "WIFI:S:   ;T:WPA;P:password;;",
            "WIFI:S:Office;T:vendor-private;P:password;;",
            "WIFI:S:Office;T:WPA2-EAP;P:password;;",
            "WIFI:S:Office;T:WPA3-EAP;P:password;;",
            "WIFI:S:Office;T:WPA;E:TTLS;P:password;;",
            "WIFI:S:Office;T:WPA;PH2:MSCHAPV2;P:password;;",
            "WIFI:S:Office;T:WPA;A:anon;P:password;;",
            "WIFI:S:Office;T:WPA;I:user;P:password;;",
            "WIFI:S:Office;T:WPA;H:yes;P:password;;",
            "WIFI:S:Office;T:WPA;P:password;",
            "WIFI:S:Office;T:WPA;P:password;;;",
            "WIFI:S:Office\\;;",
            "WIFI:S:Office;S:Other;T:WPA;P:password;;",
            "WIFI:S:Office;T:WPA;T:WEP;P:password;;",
            "WIFI:S:Office;T:WPA;P:first;P:second;;",
            "WIFI:S:Office;T:WPA;H:true;H:false;P:password;;",
            "WIFI:S:Office;broken;;",
            "not-wifi:S:Office;;",
        ).forEach { rawValue ->
            assertNull(rawValue, WifiQrPayloadParser.parse(rawValue))
        }
    }
}
