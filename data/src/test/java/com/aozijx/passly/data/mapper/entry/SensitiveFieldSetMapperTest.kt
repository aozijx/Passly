package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveFieldSetMapperTest {
    @Test
    fun `every field key is split updated redacted and restored without losing bundle metadata`() {
        val cases = populatedCases()
        assertEquals(SensitiveFieldKey.entries.toSet(), cases.flatMap { it.second.keys }.toSet())
        cases.forEach { (credential, fields, redacted) ->
            val customFields = listOf(CustomField("hidden", "retained value", CustomFieldKind.HIDDEN))
            val secret = EntrySecret(credential = credential, notes = "retained notes", customFields = customFields)
            val bundle = EntrySecret(credential = redacted, notes = "retained notes", customFields = customFields)

            assertEquals(fields, secret.toSensitiveFieldValues())
            assertEquals(fields.mapValues { SensitiveFieldUpdate.Replace(it.value) }, secret.toSensitiveFieldUpdates())
            assertEquals(bundle, secret.toBundleSecret())
            assertEquals(secret, bundle.mergeSensitiveFields(fields))
            assertEquals(secret, secret.mergeSensitiveFields(emptyMap()))
        }
    }

    @Test
    fun `unrevealed credentials retain every applicable row while exposing no field values`() {
        populatedCases().forEach { (_, fields, credential) ->
            val secret = EntrySecret(credential = credential)

            assertEquals(emptyMap<SensitiveFieldKey, String>(), secret.toSensitiveFieldValues())
            assertEquals(fields.mapValues { SensitiveFieldUpdate.Preserve }, secret.toSensitiveFieldUpdates())
            assertFalse(secret.hasStoredChanges(secret, fields.keys))
        }
    }

    @Test
    fun `blank scalar fields clear rows while blank recovery codes retain their wire representation`() {
        val cases =
            listOf(
                LoginCredential(password = " ") to setOf(SensitiveFieldKey.PASSWORD),
                CardCredential(cardNumber = "", cardCvv = " ", paymentPin = "\t") to
                    setOf(SensitiveFieldKey.CARD_NUMBER, SensitiveFieldKey.CARD_CVV, SensitiveFieldKey.CARD_PAYMENT_PIN),
                IdentityCredential(idNumber = "", seedPhrase = " ", recoveryCodes = listOf("", " ")) to
                    setOf(SensitiveFieldKey.IDENTITY_NUMBER, SensitiveFieldKey.SEED_PHRASE, SensitiveFieldKey.RECOVERY_CODES),
                SshCredential(privateKey = "", passphrase = " ") to
                    setOf(SensitiveFieldKey.SSH_PRIVATE_KEY, SensitiveFieldKey.SSH_PASSPHRASE),
                PasskeyCredential(privateKeyReference = " ") to setOf(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE),
            )
        cases.forEach { (credential, keys) ->
            val secret = EntrySecret(credential = credential)
            val expectedValues =
                if (credential is IdentityCredential) {
                    mapOf(SensitiveFieldKey.RECOVERY_CODES to "\n ")
                } else {
                    emptyMap()
                }
            assertEquals(expectedValues, secret.toSensitiveFieldValues())
            assertEquals(keys.associateWith { SensitiveFieldUpdate.Clear }, secret.toSensitiveFieldUpdates())
            assertTrue(secret.hasStoredChanges(secret.toBundleSecret(), keys))
            assertFalse(secret.hasStoredChanges(secret.toBundleSecret(), emptySet()))
        }
    }

    @Test
    fun `empty recovery codes preserve the undecrypted row`() {
        val secret = EntrySecret(credential = IdentityCredential(recoveryCodes = emptyList()))
        assertEquals(emptyMap<SensitiveFieldKey, String>(), secret.toSensitiveFieldValues())
        assertEquals(SensitiveFieldUpdate.Preserve, secret.toSensitiveFieldUpdates()[SensitiveFieldKey.RECOVERY_CODES])
        assertFalse(secret.hasStoredChanges(secret, setOf(SensitiveFieldKey.RECOVERY_CODES)))
    }

    @Test
    fun `OTP rejects blank secrets before they can enter field mapping`() {
        listOf("", " ", "\t\n").forEach { blank ->
            assertThrows(IllegalArgumentException::class.java) { OtpCredential(OtpConfig.Totp(secret = blank)) }
        }
    }

    @Test
    fun `partial field restoration overrides only supplied keys and preserves existing bundle values`() {
        val credential = CardCredential(cardNumber = "old-number", cardCvv = "old-cvv", paymentPin = "old-pin")
        val secret = EntrySecret(credential = credential)
        assertEquals(
            secret.copy(credential = credential.copy(cardCvv = "")),
            secret.mergeSensitiveFields(
                mapOf(SensitiveFieldKey.CARD_CVV to "", SensitiveFieldKey.PASSWORD to "foreign"),
            ),
        )
        val identity = EntrySecret(credential = IdentityCredential(recoveryCodes = listOf("old")))
        assertEquals(
            identity.copy(credential = IdentityCredential(recoveryCodes = listOf("one", "", "two", ""))),
            identity.mergeSensitiveFields(mapOf(SensitiveFieldKey.RECOVERY_CODES to "one\n\ntwo\n")),
        )
    }

    @Test
    fun `wifi and none retain existing aggregate semantics and ignore field rows`() {
        listOf(EntryCredential.None, WifiCredential(ssid = "network", password = "wifi-secret")).forEach { credential ->
            val secret = EntrySecret(credential = credential, notes = "notes")
            assertEquals(emptyMap<SensitiveFieldKey, String>(), secret.toSensitiveFieldValues())
            assertEquals(emptyMap<SensitiveFieldKey, SensitiveFieldUpdate>(), secret.toSensitiveFieldUpdates())
            assertEquals(secret, secret.toBundleSecret())
            assertEquals(secret, secret.mergeSensitiveFields(mapOf(SensitiveFieldKey.PASSWORD to "foreign")))
        }
    }

    private fun populatedCases(): List<Triple<EntryCredential, Map<SensitiveFieldKey, String>, EntryCredential>> =
        listOf(
            Triple(
                LoginCredential(email = "email", password = " password "),
                mapOf(SensitiveFieldKey.PASSWORD to " password "),
                LoginCredential(email = "email"),
            ),
            Triple(
                CardCredential(cardNumber = "4111", cardCvv = "123", paymentPin = "4567", cardHolder = "holder"),
                mapOf(
                    SensitiveFieldKey.CARD_NUMBER to "4111",
                    SensitiveFieldKey.CARD_CVV to "123",
                    SensitiveFieldKey.CARD_PAYMENT_PIN to "4567",
                ),
                CardCredential(cardHolder = "holder"),
            ),
            Triple(
                IdentityCredential(
                    idNumber = "identity",
                    seedPhrase = "seed",
                    recoveryCodes = listOf("one", "two"),
                    securityQuestion = "question",
                    securityAnswer = "answer",
                ),
                mapOf(
                    SensitiveFieldKey.IDENTITY_NUMBER to "identity",
                    SensitiveFieldKey.SEED_PHRASE to "seed",
                    SensitiveFieldKey.RECOVERY_CODES to "one\ntwo",
                ),
                IdentityCredential(securityQuestion = "question", securityAnswer = "answer"),
            ),
            Triple(
                SshCredential(privateKey = "private", publicKey = "public", passphrase = "passphrase"),
                mapOf(SensitiveFieldKey.SSH_PRIVATE_KEY to "private", SensitiveFieldKey.SSH_PASSPHRASE to "passphrase"),
                SshCredential(publicKey = "public"),
            ),
            Triple(
                PasskeyCredential(rpId = "rp", privateKeyReference = "reference"),
                mapOf(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE to "reference"),
                PasskeyCredential(rpId = "rp"),
            ),
            Triple(
                OtpCredential(OtpConfig.Totp(secret = "otp", issuer = "issuer")),
                mapOf(SensitiveFieldKey.OTP_SECRET to "otp"),
                OtpCredential(OtpConfig.Totp(issuer = "issuer")),
            ),
        )

    @Test
    fun `unrevealed password and an absent optional clear do not create a content revision`() {
        val stored = EntrySecret(credential = CardCredential(), notes = "notes")
        assertFalse(
            stored
                .copy(credential = CardCredential(cardCvv = ""))
                .hasStoredChanges(stored, setOf(SensitiveFieldKey.CARD_NUMBER)),
        )
    }

    @Test
    fun `clearing an existing high field and explicitly replacing it are content changes`() {
        val stored = EntrySecret(credential = CardCredential())
        assertTrue(
            stored
                .copy(credential = CardCredential(cardCvv = ""))
                .hasStoredChanges(stored, setOf(SensitiveFieldKey.CARD_CVV)),
        )
        assertTrue(
            stored
                .copy(credential = CardCredential(cardCvv = "123"))
                .hasStoredChanges(stored, emptySet()),
        )
    }

    @Test
    fun `bundle changes create a revision while identical metadata-only saves do not`() {
        val stored = EntrySecret(notes = "before")
        assertFalse(stored.hasStoredChanges(stored, emptySet()))
        assertTrue(stored.copy(notes = "after").hasStoredChanges(stored, emptySet()))
    }

    @Test
    fun `missing sensitive value is preserved during an entry update`() {
        val updates =
            EntrySecret(
                credential = LoginCredential(password = null),
            ).toSensitiveFieldUpdates()

        assertEquals(
            SensitiveFieldUpdate.Preserve,
            updates[SensitiveFieldKey.PASSWORD],
        )
    }

    @Test
    fun `blank sensitive value is cleared during an entry update`() {
        val updates =
            EntrySecret(
                credential = CardCredential(cardCvv = ""),
            ).toSensitiveFieldUpdates()

        assertEquals(
            SensitiveFieldUpdate.Clear,
            updates[SensitiveFieldKey.CARD_CVV],
        )
    }

    @Test
    fun `provided sensitive value replaces only that field during an entry update`() {
        val updates =
            EntrySecret(
                credential = CardCredential(cardNumber = "4111"),
            ).toSensitiveFieldUpdates()

        assertEquals(
            SensitiveFieldUpdate.Replace("4111"),
            updates[SensitiveFieldKey.CARD_NUMBER],
        )
        assertEquals(
            SensitiveFieldUpdate.Preserve,
            updates[SensitiveFieldKey.CARD_CVV],
        )
        assertEquals(
            SensitiveFieldUpdate.Preserve,
            updates[SensitiveFieldKey.CARD_PAYMENT_PIN],
        )
    }

    @Test
    fun `card credential splits into independent field values`() {
        val fields =
            EntrySecret(
                credential = CardCredential(cardNumber = "4111", cardCvv = "123"),
            ).toSensitiveFieldValues()

        assertEquals("4111", fields[SensitiveFieldKey.CARD_NUMBER])
        assertEquals("123", fields[SensitiveFieldKey.CARD_CVV])
        assertNull(fields[SensitiveFieldKey.CARD_PAYMENT_PIN])
    }

    @Test
    fun `identity recovery codes use one stable storage value`() {
        val fields =
            EntrySecret(
                credential = IdentityCredential(recoveryCodes = listOf("one", "two")),
            ).toSensitiveFieldValues()

        assertEquals("one\ntwo", fields[SensitiveFieldKey.RECOVERY_CODES])
    }
}
