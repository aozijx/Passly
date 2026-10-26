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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryFieldEditorTest {
    private val card = CardCredential(cardHolder = "Before", cardNumber = "4111111111111111", cardCvv = "123", bankBranch = "Old branch")
    private val entry = Entry(
        EntryIdentity(EntryId("card"), EntryType.BANK_CARD, timestamps = EntryTimestamps(1)),
        EntryProfile(title = "Card", username = "profile-user"), EntrySecret(card, notes = "Keep notes"),
    )

    @Test
    fun `card number edits reject missing invalid and malformed values`() {
        for (value in listOf("", "4111111111111112", "4111x111111111111")) {
            assertFalse(EntryFieldEditor.edit(entry, FieldKey.CARD_NUMBER, value) is EntryEditResult.Updated)
        }
    }

    @Test
    fun `optional cvv edits validate only the changed field and allow clearing`() {
        val redacted = entry.copy(secret = entry.secret.copy(credential = card.copy(cardNumber = null, cardCvv = null)))
        assertFalse(EntryFieldEditor.edit(redacted, FieldKey.CARD_CVV, "12") is EntryEditResult.Updated)
        for (value in listOf("123", "")) {
            val updated = EntryFieldEditor.edit(redacted, FieldKey.CARD_CVV, value) as EntryEditResult.Updated
            assertEquals(redacted.secret.card?.copy(cardCvv = value), updated.entry.secret.card)
        }
    }

    @Test
    fun `password edits follow the corresponding credential rule`() {
        val login = entry.copy(identity = entry.identity.copy(type = EntryType.LOGIN), secret = EntrySecret(LoginCredential()))
        val wifi = entry.copy(identity = entry.identity.copy(type = EntryType.WIFI), secret = EntrySecret(WifiCredential("Network")))
        assertFalse(EntryFieldEditor.edit(login, FieldKey.PASSWORD, "  ") is EntryEditResult.Updated)
        assertFalse(EntryFieldEditor.edit(wifi, FieldKey.PASSWORD, "short") is EntryEditResult.Updated)
        assertTrue(EntryFieldEditor.edit(wifi, FieldKey.PASSWORD, "valid-password") is EntryEditResult.Updated)
    }

    @Test
    fun `each card metadata edit changes only its logical field`() {
        val expected = listOf(
            FieldKey.CARD_HOLDER to card.copy(cardHolder = "Updated"),
            FieldKey.CARD_ISSUING_BANK to card.copy(issuingBank = "Updated"),
            FieldKey.CARD_BANK_BRANCH to card.copy(bankBranch = "Updated"),
            FieldKey.CARD_PRODUCT to card.copy(cardProduct = "Updated"),
            FieldKey.CARD_CURRENCY to card.copy(currency = "Updated"),
            FieldKey.CARD_BANK_PHONE to card.copy(bankPhone = "Updated"),
        )
        for ((field, credential) in expected) {
            val result = EntryFieldEditor.edit(entry, field, " Updated ") as EntryEditResult.Updated
            assertEquals(entry.copy(secret = entry.secret.copy(credential = credential)), result.entry)
        }
    }

    @Test
    fun `clearing optional metadata preserves unrevealed sensitive fields`() {
        val redacted = entry.copy(secret = entry.secret.copy(credential = card.copy(cardNumber = null, cardCvv = null)))
        val result = EntryFieldEditor.edit(redacted, FieldKey.CARD_BANK_BRANCH, "  ") as EntryEditResult.Updated
        assertEquals(redacted.secret.card?.copy(bankBranch = null), result.entry.secret.card)
    }

    @Test
    fun `fields from another credential type are rejected without creating a replacement credential`() {
        val login = entry.copy(identity = entry.identity.copy(type = EntryType.LOGIN), secret = EntrySecret(LoginCredential(password = "keep")))
        assertEquals(EntryEditResult.Unsupported, EntryFieldEditor.edit(login, FieldKey.CARD_NUMBER, "5555555555554444"))
        assertEquals(EntryEditResult.Unsupported, EntryFieldEditor.edit(entry, FieldKey.PASSWORD, "new"))
        assertEquals(EntryEditResult.Unsupported, EntryFieldEditor.edit(entry, FieldKey.USERNAME, "new"))
    }

    @Test
    fun `edit result does not stringify credential or profile data`() {
        val result = EntryFieldEditor.edit(entry, FieldKey.CARD_HOLDER, "Private Holder")
        assertFalse(result.toString().contains("Private Holder"))
        assertFalse(result.toString().contains("4111111111111111"))
        assertFalse(result.toString().contains("profile-user"))
    }
}
