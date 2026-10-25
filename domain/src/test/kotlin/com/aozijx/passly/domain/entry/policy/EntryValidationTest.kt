package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidationTest {
    @Test
    fun `card expiry accepts the four digit year emitted by the editor`() {
        val violations = EntryValidation.validate(entry(
            type = EntryType.BANK_CARD,
            profile = EntryProfile(title = "Card"),
            secret = EntrySecret(CardCredential(cardNumber = "4111111111111111", cardExpiry = "03/2030")),
        ))

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `card expiry rejects invalid months and ambiguous short years`() {
        listOf("00/30", "13/30", "03/30", "00/2030", "13/2030", "03/0000").forEach { expiry ->
            val violations = EntryValidation.validate(entry(
                type = EntryType.BANK_CARD,
                profile = EntryProfile(title = "Card"),
                secret = EntrySecret(CardCredential(cardNumber = "4111111111111111", cardExpiry = expiry)),
            ))
            assertTrue(EntryViolation(FieldKey.CARD_EXPIRATION, EntryViolationCode.INVALID_FORMAT) in violations)
        }
    }

    @Test
    fun `login validation keeps optional username and returns stable password code`() {
        val violations = EntryValidation.validate(
            entry(
                type = EntryType.LOGIN,
                profile = EntryProfile(title = "Login"),
                secret = EntrySecret(LoginCredential()),
            )
        )

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.REQUIRED)),
            violations,
        )
    }

    @Test
    fun `card validation reports independent field violations`() {
        val violations = EntryValidation.validate(
            entry(
                type = EntryType.BANK_CARD,
                profile = EntryProfile(title = "Card"),
                secret = EntrySecret(
                    CardCredential(
                        cardNumber = "123",
                        cardExpiry = "2028-10",
                        cardCvv = "12",
                    )
                ),
            )
        )

        assertTrue(EntryViolation(FieldKey.CARD_NUMBER, EntryViolationCode.INVALID_LENGTH) in violations)
        assertTrue(EntryViolation(FieldKey.CARD_EXPIRATION, EntryViolationCode.INVALID_FORMAT) in violations)
        assertTrue(EntryViolation(FieldKey.CARD_CVV, EntryViolationCode.INVALID_FORMAT) in violations)
    }

    @Test
    fun `open wifi rejects a retained password`() {
        val violations = WifiCredentialValidation.validate(
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
            assertTrue(
                WifiCredentialValidation.validate("Legacy", password, WifiSecurityType.Wep).isEmpty(),
            )
        }

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH)),
            WifiCredentialValidation.validate("Legacy", "12345678", WifiSecurityType.Wep),
        )
    }

    @Test
    fun `wpa accepts passphrases and 64 digit hexadecimal keys`() {
        listOf("12345678", "p".repeat(63), "A1".repeat(32)).forEach { password ->
            assertTrue(
                WifiCredentialValidation.validate("Office", password, WifiSecurityType.WpaPersonal).isEmpty(),
            )
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

    @Test
    fun `entry validation delegates wifi rules to the shared policy`() {
        val violations = EntryValidation.validate(
            entry(
                type = EntryType.WIFI,
                profile = EntryProfile(title = "Office"),
                secret = EntrySecret(
                    WifiCredential(
                        ssid = "Office",
                        password = "short",
                        securityType = WifiSecurityType.WpaPersonal,
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH)),
            violations,
        )
    }

    private fun entry(type: EntryType, profile: EntryProfile, secret: EntrySecret) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = type,
            timestamps = EntryTimestamps(createdAtMs = 1),
        ),
        profile = profile,
        secret = secret,
    )
}
