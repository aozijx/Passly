package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions

fun AddBankCardFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.BANK_CARD]
    var draft = EntryDraft(EntryDraftTarget.New(EntryType.BANK_CARD))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.CARD_NUMBER, EntryDraftValue.Text(cardNumber.trim()))

    draft = cardType?.let {
        draft.withValue(definition, FieldKey.CARD_TYPE, EntryDraftValue.Text(it.name))
    } ?: draft
    draft = draft.withOptionalText(definition, FieldKey.CARD_HOLDER, cardholder)
    draft = draft.withOptionalText(definition, FieldKey.CARD_CVV, cardCvv)
    draft = draft.withOptionalText(definition, FieldKey.PAYMENT_PIN, paymentPin)
    draft = draft.withOptionalText(definition, FieldKey.BILLING_ADDRESS, billingAddress)
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    normalizedCardExpiry(cardExpiryMonth, cardExpiryYear)?.let {
        draft = draft.withValue(definition, FieldKey.CARD_EXPIRATION, EntryDraftValue.Text(it))
    }
    return draft.withTags(definition, tags)
}

internal fun normalizedCardExpiry(month: String, year: String): String? {
    val normalizedMonth = month.trim()
    val normalizedYear = year.trim()
    if (normalizedMonth.isEmpty() && normalizedYear.isEmpty()) return null
    return "$normalizedMonth/$normalizedYear"
}

private fun EntryDraft.withOptionalText(
    definition: EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft = rawValue.trim().takeIf(String::isNotEmpty)
    ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
    ?: this

private fun EntryDraft.withTags(definition: EntryTypeDefinition, rawTags: String): EntryDraft {
    val values = rawTags.split(',').map(String::trim).filter(String::isNotEmpty)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
