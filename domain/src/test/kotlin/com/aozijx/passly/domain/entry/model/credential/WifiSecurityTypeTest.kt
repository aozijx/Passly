package com.aozijx.passly.domain.entry.model.credential

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiSecurityTypeTest {
    @Test
    fun `legacy blank storage keeps wpa personal semantics`() {
        listOf(null, "", "   ").forEach { rawValue ->
            assertEquals(WifiSecurityType.WpaPersonal, WifiSecurityType.fromStoredValue(rawValue))
        }
    }

    @Test
    fun `known stored aliases map without depending on kotlin names`() {
        mapOf(
            "nopass" to WifiSecurityType.Open,
            "OPEN" to WifiSecurityType.Open,
            "WEP" to WifiSecurityType.Wep,
            "WPA" to WifiSecurityType.WpaPersonal,
            "wpa2" to WifiSecurityType.WpaPersonal,
            "WPA3" to WifiSecurityType.WpaPersonal,
            "SAE" to WifiSecurityType.WpaPersonal,
        ).forEach { (rawValue, expected) ->
            assertEquals(expected, WifiSecurityType.fromStoredValue(rawValue))
        }
    }

    @Test
    fun `unknown storage values use the supported default`() {
        assertEquals(
            WifiSecurityType.WpaPersonal,
            WifiSecurityType.fromStoredValue("vendor-private"),
        )
    }

    @Test
    fun `supported types expose stable stored values`() {
        assertEquals("nopass", WifiSecurityType.Open.storedValue)
        assertEquals("WEP", WifiSecurityType.Wep.storedValue)
        assertEquals("WPA", WifiSecurityType.WpaPersonal.storedValue)
    }
}
