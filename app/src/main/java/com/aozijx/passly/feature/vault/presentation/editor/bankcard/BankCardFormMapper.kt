package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import com.aozijx.passly.feature.vault.presentation.editor.parseRawTags

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.model.credential.CardExpiryParseResult

fun AddBankCardFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.BANK_CARD]
    var draft = EntryDraft(EntryType.BANK_CARD)
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.CARD_NUMBER, EntryDraftValue.Text(cardNumber.trim()))

    draft = when (val selection = cardType) {
        CardTypeSelection.Unselected -> draft
        is CardTypeSelection.Selected -> draft.withValue(
            definition,
            FieldKey.CARD_TYPE,
            EntryDraftValue.Text(when (selection.type) {
                CardType.DEBIT -> "DEBIT"
                CardType.CREDIT -> "CREDIT"
            }),
        )
    }
    draft = draft.withOptionalText(definition, FieldKey.CARD_HOLDER, cardholder)
    draft = draft.withOptionalText(definition, FieldKey.CARD_CVV, cardCvv)
    draft = draft.withOptionalText(definition, FieldKey.PAYMENT_PIN, paymentPin)
    draft = draft.withOptionalText(definition, FieldKey.BILLING_ADDRESS, billingAddress)
    draft = draft.withOptionalText(definition, FieldKey.CARD_ISSUING_BANK, issuingBank)
    draft = draft.withOptionalText(definition, FieldKey.CARD_BANK_BRANCH, bankBranch)
    draft = draft.withOptionalText(definition, FieldKey.CARD_PRODUCT, cardProduct)
    draft = draft.withOptionalText(definition, FieldKey.CARD_CURRENCY, currency)
    draft = draft.withOptionalText(definition, FieldKey.CARD_BANK_PHONE, bankPhone)
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    when (val selection = cardExpiry) {
        CardExpirySelection.NotSet -> Unit
        is CardExpirySelection.Selected -> {
            draft = draft.withValue(
                definition,
                FieldKey.CARD_EXPIRATION,
                EntryDraftValue.Text(selection.value.storageValue),
            )
        }
    }
    return draft.withTags(definition, tags)
}

internal fun AddBankCardFormState.fromEntry(entry: Entry): AddBankCardFormState {
    val card = entry.secret.card
    val type = when (card?.cardType) {
        "DEBIT" -> CardTypeSelection.Selected(CardType.DEBIT)
        "CREDIT" -> CardTypeSelection.Selected(CardType.CREDIT)
        else -> CardTypeSelection.Unselected
    }
    val expiry = card?.cardExpiry?.let(CardExpiry::parse)
    return copy(
        title = entry.title,
        cardType = type,
        cardholder = card?.cardHolder.orEmpty(),
        cardNumber = card?.cardNumber.orEmpty(),
        cardCvv = card?.cardCvv.orEmpty(),
        paymentPin = card?.paymentPin.orEmpty(),
        cardExpiry = (expiry as? CardExpiryParseResult.Parsed)?.value?.let(CardExpirySelection::Selected)
            ?: CardExpirySelection.NotSet,
        billingAddress = card?.billingAddress.orEmpty(),
        issuingBank = card?.issuingBank.orEmpty(),
        bankBranch = card?.bankBranch.orEmpty(),
        cardProduct = card?.cardProduct.orEmpty(),
        currency = card?.currency.orEmpty(),
        bankPhone = card?.bankPhone.orEmpty(),
        tags = entry.tags.joinToString(", "),
        notes = entry.secret.notes.orEmpty(),
        isCardNumberVisible = false,
        isCvvVisible = false,
        isPinVisible = false,
    )
}

private fun EntryDraft.withOptionalText(
    definition: EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft = rawValue.trim().takeIf(String::isNotEmpty)
    ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
    ?: this

private fun EntryDraft.withTags(definition: EntryTypeDefinition, rawTags: String): EntryDraft {
    val values = parseRawTags(rawTags)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
