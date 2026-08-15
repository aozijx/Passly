package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldDefinition
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryTypeDefinitionsTest {
    @Test
    fun `catalog defines every entry type with one required title`() {
        assertEquals(EntryType.entries.toSet(), EntryTypeDefinitions.all.map { it.type }.toSet())
        EntryTypeDefinitions.all.forEach { definition ->
            assertTrue(definition.supports(FieldKey.TITLE))
            assertTrue(FieldKey.TITLE in definition.requiredFields)
        }
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
    fun `existing draft target carries optimistic lock identity`() {
        val target = EntryDraftTarget.Existing(
            entryId = EntryId("entry-1"),
            type = EntryType.LOGIN,
            expectedVersion = EntryVersion(4),
        )

        assertEquals(EntryId("entry-1"), target.entryId)
        assertEquals(EntryType.LOGIN, target.type)
        assertEquals(EntryVersion(4), target.expectedVersion)
    }

    @Test
    fun `draft accepts only fields and value types declared by its entry type`() {
        val definition = EntryTypeDefinitions[EntryType.WIFI]
        val draft = EntryDraft(EntryDraftTarget.New(EntryType.WIFI))
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Office"))
            .withValue(definition, FieldKey.WIFI_SSID, EntryDraftValue.Text("Passly"))
            .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text("secret123"))

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
        val draft = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Example"))
            .withValue(definition, FieldKey.USERNAME, EntryDraftValue.Text(""))

        assertEquals(
            setOf(FieldKey.USERNAME, FieldKey.PASSWORD),
            draft.missingRequiredFields(definition),
        )
    }
}
