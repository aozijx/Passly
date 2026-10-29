package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object BankCardEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.BANK_CARD,
        listOf(
            titleField(),
            field(FieldKey.CARD_TYPE),
            secret(FieldKey.CARD_HOLDER),
            secret(FieldKey.CARD_ISSUING_BANK),
            secret(FieldKey.CARD_BANK_BRANCH),
            secret(FieldKey.CARD_PRODUCT),
            secret(FieldKey.CARD_CURRENCY),
            secret(FieldKey.CARD_BANK_PHONE),
            high(FieldKey.CARD_NUMBER, SensitiveFieldKey.CARD_NUMBER, required = true),
            secret(FieldKey.CARD_EXPIRATION),
            high(FieldKey.CARD_CVV, SensitiveFieldKey.CARD_CVV),
            high(FieldKey.PAYMENT_PIN, SensitiveFieldKey.CARD_PAYMENT_PIN),
            field(FieldKey.PAYMENT_PLATFORM),
            secret(FieldKey.BILLING_ADDRESS),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
