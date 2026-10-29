package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveFieldSetMapperTest {
    @Test
    fun `unrevealed password and an absent optional clear do not create a content revision`() {
        val stored = EntrySecret(credential = CardCredential(), notes = "notes")
        assertFalse(stored.copy(credential = CardCredential(cardCvv = ""))
            .hasStoredChanges(stored, setOf(SensitiveFieldKey.CARD_NUMBER)))
    }

    @Test
    fun `clearing an existing high field and explicitly replacing it are content changes`() {
        val stored = EntrySecret(credential = CardCredential())
        assertTrue(stored.copy(credential = CardCredential(cardCvv = ""))
            .hasStoredChanges(stored, setOf(SensitiveFieldKey.CARD_CVV)))
        assertTrue(stored.copy(credential = CardCredential(cardCvv = "123"))
            .hasStoredChanges(stored, emptySet()))
    }

    @Test
    fun `bundle changes create a revision while identical metadata-only saves do not`() {
        val stored = EntrySecret(notes = "before")
        assertFalse(stored.hasStoredChanges(stored, emptySet()))
        assertTrue(stored.copy(notes = "after").hasStoredChanges(stored, emptySet()))
    }

    @Test
    fun `missing sensitive value is preserved during an entry update`() {
        val updates = EntrySecret(
            credential = LoginCredential(password = null),
        ).toSensitiveFieldUpdates()

        assertEquals(
            SensitiveFieldUpdate.Preserve,
            updates[SensitiveFieldKey.PASSWORD],
        )
    }

    @Test
    fun `blank sensitive value is cleared during an entry update`() {
        val updates = EntrySecret(
            credential = CardCredential(cardCvv = ""),
        ).toSensitiveFieldUpdates()

        assertEquals(
            SensitiveFieldUpdate.Clear,
            updates[SensitiveFieldKey.CARD_CVV],
        )
    }

    @Test
    fun `provided sensitive value replaces only that field during an entry update`() {
        val updates = EntrySecret(
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
        val fields = EntrySecret(
            credential = CardCredential(cardNumber = "4111", cardCvv = "123"),
        ).toSensitiveFieldValues()

        assertEquals("4111", fields[SensitiveFieldKey.CARD_NUMBER])
        assertEquals("123", fields[SensitiveFieldKey.CARD_CVV])
        assertNull(fields[SensitiveFieldKey.CARD_PAYMENT_PIN])
    }

    @Test
    fun `identity recovery codes use one stable storage value`() {
        val fields = EntrySecret(
            credential = IdentityCredential(recoveryCodes = listOf("one", "two")),
        ).toSensitiveFieldValues()

        assertEquals("one\ntwo", fields[SensitiveFieldKey.RECOVERY_CODES])
    }
}
