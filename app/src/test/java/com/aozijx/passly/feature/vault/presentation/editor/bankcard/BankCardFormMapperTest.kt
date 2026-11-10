package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankCardFormMapperTest {
    @Test
    fun fromEntryRestoresProtectedAndOptionalCardFields() {
        val entry =
            Entry(
                EntryIdentity(EntryId("card"), EntryType.BANK_CARD, timestamps = EntryTimestamps(1)),
                EntryProfile("Card", tags = setOf("work")),
                EntrySecret(
                    credential =
                        CardCredential(
                            cardType = "CREDIT",
                            cardNumber = "4111111111111111",
                            cardExpiry = "03/2030",
                            cardCvv = "123",
                            cardHolder = "Ada",
                            paymentPin = "4567",
                            billingAddress = "London",
                            issuingBank = "Bank",
                            bankBranch = "Central",
                            cardProduct = "Travel",
                            currency = "GBP",
                            bankPhone = "+44 123456",
                        ),
                    notes = "note",
                ),
            )

        val form =
            AddBankCardFormState(
                isCardNumberVisible = true,
                isCvvVisible = true,
                isPinVisible = true,
            ).fromEntry(entry)
        val draft = form.toEntryDraft()

        assertEquals("Card", form.title)
        assertEquals(CardTypeSelection.Selected(CardType.CREDIT), form.cardTypeSelection)
        assertEquals("Ada", form.cardholder)
        assertEquals("4111111111111111", form.cardNumber)
        assertEquals("123", form.cardCvv)
        assertEquals("4567", form.paymentPin)
        assertEquals(CardExpirySelection.Selected(CardExpiry(month = 3, year = 2030)), form.cardExpirySelection)
        assertEquals("London", form.billingAddress)
        assertEquals("Bank", form.issuingBank)
        assertEquals("Central", form.bankBranch)
        assertEquals("Travel", form.cardProduct)
        assertEquals("GBP", form.currency)
        assertEquals("+44 123456", form.bankPhone)
        assertEquals("work", form.tags)
        assertEquals("note", form.notes)
        assertEquals(false, form.isCardNumberVisible)
        assertEquals(false, form.isCvvVisible)
        assertEquals(false, form.isPinVisible)
        assertEquals(EntryDraftValue.Text("Card"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("CREDIT"), draft[FieldKey.CARD_TYPE])
        assertEquals(EntryDraftValue.Text("Ada"), draft[FieldKey.CARD_HOLDER])
        assertEquals(EntryDraftValue.Text("4111111111111111"), draft[FieldKey.CARD_NUMBER])
        assertEquals(EntryDraftValue.Text("123"), draft[FieldKey.CARD_CVV])
        assertEquals(EntryDraftValue.Text("4567"), draft[FieldKey.PAYMENT_PIN])
        assertEquals(EntryDraftValue.Text("03/2030"), draft[FieldKey.CARD_EXPIRATION])
        assertEquals(EntryDraftValue.Text("London"), draft[FieldKey.BILLING_ADDRESS])
        assertEquals(EntryDraftValue.Text("Bank"), draft[FieldKey.CARD_ISSUING_BANK])
        assertEquals(EntryDraftValue.Text("Central"), draft[FieldKey.CARD_BANK_BRANCH])
        assertEquals(EntryDraftValue.Text("Travel"), draft[FieldKey.CARD_PRODUCT])
        assertEquals(EntryDraftValue.Text("GBP"), draft[FieldKey.CARD_CURRENCY])
        assertEquals(EntryDraftValue.Text("+44 123456"), draft[FieldKey.CARD_BANK_PHONE])
        assertEquals(EntryDraftValue.TextList(listOf("work")), draft[FieldKey.TAGS])
        assertEquals(EntryDraftValue.Text("note"), draft[FieldKey.NOTES])
    }

    @Test
    fun toEntryDraft_mapsCardFieldsAndNormalizesExpiry() {
        val draft =
            AddBankCardFormState(
                title = " Personal Visa ",
                cardTypeSelection = CardTypeSelection.Selected(CardType.CREDIT),
                cardholder = " Ada Lovelace ",
                cardNumber = " 4111 1111 1111 1111 ",
                cardCvv = " 1 23 ",
                paymentPin = " 1 2 3 4 ",
                cardExpirySelection = CardExpirySelection.Selected(CardExpiry(month = 3, year = 2030)),
                billingAddress = " London ",
                issuingBank = " Example Bank ",
                bankBranch = " Central Branch ",
                cardProduct = " Travel Platinum ",
                currency = " CNY / USD ",
                bankPhone = " +86 123456 ",
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
        assertEquals(EntryDraftValue.Text("Example Bank"), draft[FieldKey.CARD_ISSUING_BANK])
        assertEquals(EntryDraftValue.Text("Central Branch"), draft[FieldKey.CARD_BANK_BRANCH])
        assertEquals(EntryDraftValue.Text("Travel Platinum"), draft[FieldKey.CARD_PRODUCT])
        assertEquals(EntryDraftValue.Text("CNY / USD"), draft[FieldKey.CARD_CURRENCY])
        assertEquals(EntryDraftValue.Text("+86 123456"), draft[FieldKey.CARD_BANK_PHONE])
        assertEquals(EntryDraftValue.TextList(listOf("personal", "card")), draft[FieldKey.TAGS])
        assertEquals(EntryDraftValue.Text("primary"), draft[FieldKey.NOTES])
        assertTrue(draft.missingRequiredFields(EntryTypeDefinitions[EntryType.BANK_CARD]).isEmpty())
    }

    @Test
    fun toEntryDraft_omitsBlankOptionalCardFields() {
        val draft =
            AddBankCardFormState(
                title = "Card",
                cardNumber = "4111",
                cardExpirySelection = CardExpirySelection.NotSet,
            ).toEntryDraft()

        assertNull(draft[FieldKey.CARD_EXPIRATION])
        assertNull(draft[FieldKey.CARD_TYPE])
        assertNull(draft[FieldKey.CARD_CVV])
        assertNull(draft[FieldKey.PAYMENT_PIN])
        assertNull(draft[FieldKey.BILLING_ADDRESS])
    }
}
