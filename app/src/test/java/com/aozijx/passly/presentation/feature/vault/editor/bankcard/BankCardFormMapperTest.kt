package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankCardFormMapperTest {
    @Test
    fun toEntryDraft_mapsCardFieldsAndNormalizesExpiry() {
        val draft = AddBankCardFormState(
            title = " Personal Visa ",
            cardType = CardType.CREDIT,
            cardholder = " Ada Lovelace ",
            cardNumber = " 4111 1111 1111 1111 ",
            cardCvv = " 1 23 ",
            paymentPin = " 1 2 3 4 ",
            cardExpiryMonth = " 03 ",
            cardExpiryYear = " 2030 ",
            billingAddress = " London ",
            tags = " personal, card ",
            notes = " primary ",
        ).toEntryDraft()

        assertEquals(EntryDraftValue.Text("Personal Visa"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("CREDIT"), draft[FieldKey.CARD_TYPE])
        assertEquals(EntryDraftValue.Text("Ada Lovelace"), draft[FieldKey.CARD_HOLDER])
        assertEquals(EntryDraftValue.Text("4111 1111 1111 1111"), draft[FieldKey.CARD_NUMBER])
        assertEquals(EntryDraftValue.Text("1 23"), draft[FieldKey.CARD_CVV])
        assertEquals(EntryDraftValue.Text("1 2 3 4"), draft[FieldKey.PAYMENT_PIN])
        assertEquals(EntryDraftValue.Text("03/2030"), draft[FieldKey.CARD_EXPIRATION])
        assertEquals(EntryDraftValue.Text("London"), draft[FieldKey.BILLING_ADDRESS])
        assertEquals(EntryDraftValue.TextList(listOf("personal", "card")), draft[FieldKey.TAGS])
        assertEquals(EntryDraftValue.Text("primary"), draft[FieldKey.NOTES])
        assertTrue(draft.missingRequiredFields(EntryTypeDefinitions[EntryType.BANK_CARD]).isEmpty())
    }

    @Test
    fun toEntryDraft_omitsBlankOptionalCardFields() {
        val draft = AddBankCardFormState(
            title = "Card",
            cardNumber = "4111",
            cardExpiryMonth = " ",
            cardExpiryYear = " ",
        ).toEntryDraft()

        assertNull(draft[FieldKey.CARD_EXPIRATION])
        assertNull(draft[FieldKey.CARD_CVV])
        assertNull(draft[FieldKey.PAYMENT_PIN])
        assertNull(draft[FieldKey.BILLING_ADDRESS])
    }
}
