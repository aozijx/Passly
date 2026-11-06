package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiCredentialValidationTest {
    @Test
    fun `open wifi rejects a retained password`() {
        val violations =
            WifiCredentialValidation.validate(
                ssid = "Guest",
                password = "must-be-cleared",
                securityType = WifiSecurityType.Open,
            )

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_FORMAT)),
            violations,
        )
    }

    @Test
    fun `wep accepts only standard ascii or hexadecimal lengths`() {
        listOf("abcde", "abcdefghijklm", "0011223344", "00112233445566778899AABBCC").forEach { password ->
            assertTrue(WifiCredentialValidation.validate("Legacy", password, WifiSecurityType.Wep).isEmpty())
        }

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH)),
            WifiCredentialValidation.validate("Legacy", "12345678", WifiSecurityType.Wep),
        )
    }

    @Test
    fun `wpa accepts passphrases and 64 digit hexadecimal keys`() {
        listOf("12345678", "p".repeat(63), "A1".repeat(32)).forEach { password ->
            assertTrue(WifiCredentialValidation.validate("Office", password, WifiSecurityType.WpaPersonal).isEmpty())
        }

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH)),
            WifiCredentialValidation.validate("Office", "short", WifiSecurityType.WpaPersonal),
        )
    }

    @Test
    fun `wifi validation reports blank ssid`() {
        assertEquals(
            setOf(EntryViolation(FieldKey.WIFI_SSID, EntryViolationCode.REQUIRED)),
            WifiCredentialValidation.validate(
                ssid = "  ",
                password = "password",
                securityType = WifiSecurityType.WpaPersonal,
            ),
        )
    }
}
