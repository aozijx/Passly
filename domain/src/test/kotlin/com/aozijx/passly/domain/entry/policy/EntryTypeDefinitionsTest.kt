package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryTypeDefinitionsTest {

    @Test
    fun sensitiveStorageKeyIsResolvedWithinEntryType() {
        assertEquals(
            SensitiveFieldKey.PASSWORD,
            EntryTypeDefinitions.sensitiveStorageKey(EntryType.LOGIN, FieldKey.PASSWORD),
        )
        assertEquals(null, EntryTypeDefinitions.sensitiveStorageKey(EntryType.WIFI, FieldKey.PASSWORD))
    }
    @Test
    fun otpDefinition_usesOtpScopedIdentityFields() {
        val otp = EntryTypeDefinitions[EntryType.OTP]

        assertTrue(FieldKey.OTP_ISSUER in otp.fields.map { it.key })
        assertTrue(FieldKey.OTP_ACCOUNT_NAME in otp.fields.map { it.key })
        assertTrue(FieldKey.OTP_ENCODING in otp.fields.map { it.key })
        assertFalse(FieldKey.USERNAME in otp.fields.map { it.key })
        assertFalse(FieldKey.PASSWORD in otp.fields.map { it.key })
        assertFalse(FieldKey.PRIMARY_URL in otp.fields.map { it.key })
        assertFalse(FieldKey.DOMAINS in otp.fields.map { it.key })
    }

    @Test
    fun loginDefinition_allowsPasswordOnlyCredentials() {
        val login = EntryTypeDefinitions[EntryType.LOGIN]

        assertFalse(FieldKey.USERNAME in login.requiredFields)
        assertTrue(FieldKey.PASSWORD in login.requiredFields)
    }

    @Test
    fun otpDefinition_preservesHotpCounterAsLong() {
        val definition = EntryTypeDefinitions[EntryType.OTP]
        val counter = requireNotNull(definition[FieldKey.OTP_COUNTER])

        assertEquals(EntryFieldValueType.LONG, counter.valueType)
        val draft = EntryDraft(EntryType.OTP).withValue(
            definition,
            FieldKey.OTP_COUNTER,
            EntryDraftValue.LongNumber(Long.MAX_VALUE),
        )
        assertEquals(EntryDraftValue.LongNumber(Long.MAX_VALUE), draft[FieldKey.OTP_COUNTER])
    }
    @Test
    fun `catalog defines every entry type with one required title`() {
        assertEquals(EntryType.entries.toSet(), EntryTypeDefinitions.all.map { it.type }.toSet())
        EntryTypeDefinitions.all.forEach { definition ->
            assertTrue(definition.supports(FieldKey.TITLE))
            assertTrue(FieldKey.TITLE in definition.requiredFields)
        }
    }

    @Test
    fun `catalog rejects duplicate type definitions before indexing`() {
        val login = EntryTypeDefinition(
            EntryType.LOGIN,
            listOf(EntryFieldDefinition(FieldKey.TITLE, required = true)),
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            buildEntryTypeDefinitionIndex(
                definitions = listOf(login, login),
                expectedTypes = setOf(EntryType.LOGIN),
            )
        }

        assertTrue(error.message.orEmpty().contains(EntryType.LOGIN.name))
    }

    @Test
    fun `catalog rejects missing type definitions with the missing type identified`() {
        val login = EntryTypeDefinition(
            EntryType.LOGIN,
            listOf(EntryFieldDefinition(FieldKey.TITLE, required = true)),
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            buildEntryTypeDefinitionIndex(
                definitions = listOf(login),
                expectedTypes = setOf(EntryType.LOGIN, EntryType.OTP),
            )
        }

        assertTrue(error.message.orEmpty().contains(EntryType.OTP.name))
    }

    @Test
    fun `credential types expose their own semantic fields`() {
        val login = EntryTypeDefinitions[EntryType.LOGIN]
        val card = EntryTypeDefinitions[EntryType.BANK_CARD]
        val wifi = EntryTypeDefinitions[EntryType.WIFI]
        val ssh = EntryTypeDefinitions[EntryType.SSH_KEY]

        assertTrue(login.supports(FieldKey.USERNAME))
        assertTrue(login.supports(FieldKey.PASSWORD))
        assertTrue(card.supports(FieldKey.CARD_NUMBER))
        assertTrue(card.supports(FieldKey.CARD_CVV))
        assertTrue(wifi.supports(FieldKey.WIFI_SSID))
        assertTrue(wifi.supports(FieldKey.PASSWORD))
        assertTrue(FieldKey.WIFI_SSID in wifi.requiredFields)
        assertFalse(FieldKey.PASSWORD in wifi.requiredFields)
        assertTrue(ssh.supports(FieldKey.SSH_KEY))
        assertTrue(ssh.supports(FieldKey.SSH_PASSPHRASE))
        assertFalse(card.supports(FieldKey.PASSWORD))
    }

    @Test
    fun `high sensitivity definitions map to protected storage keys`() {
        val mappedKeys = EntryTypeDefinitions.all
            .flatMap(EntryTypeDefinition::fields)
            .filter { it.access == EntryFieldAccess.HIGH_SENSITIVITY }
            .mapNotNull(EntryFieldDefinition::sensitiveFieldKey)
            .toSet()

        assertEquals(SensitiveFieldKey.entries.toSet(), mappedKeys)
    }

    @Test
    fun `definition rejects ambiguous field contracts`() {
        val title = EntryFieldDefinition(FieldKey.TITLE, required = true)

        assertThrows(IllegalArgumentException::class.java) {
            EntryTypeDefinition(EntryType.LOGIN, listOf(title, title))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntryFieldDefinition(
                key = FieldKey.PASSWORD,
                access = EntryFieldAccess.HIGH_SENSITIVITY,
            )
        }
    }

    @Test
    fun `draft accepts only fields and value types declared by its entry type`() {
        val definition = EntryTypeDefinitions[EntryType.WIFI]
        val draft = EntryDraft(EntryType.WIFI)
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Office"))
            .withValue(definition, FieldKey.WIFI_SSID, EntryDraftValue.Text("Passly"))

        assertTrue(draft.missingRequiredFields(definition).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            draft.withValue(
                definition,
                FieldKey.WIFI_HIDDEN,
                EntryDraftValue.Text("true"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            draft.withValue(definition, FieldKey.CARD_NUMBER, EntryDraftValue.Text("4111"))
        }
    }

    @Test
    fun `draft reports blank required fields using the domain definition`() {
        val definition = EntryTypeDefinitions[EntryType.LOGIN]
        val draft = EntryDraft(EntryType.LOGIN)
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Example"))
            .withValue(definition, FieldKey.USERNAME, EntryDraftValue.Text(""))

        assertEquals(
            setOf(FieldKey.PASSWORD),
            draft.missingRequiredFields(definition),
        )
    }
}
