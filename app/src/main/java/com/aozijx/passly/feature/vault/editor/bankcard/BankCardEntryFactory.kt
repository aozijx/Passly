package com.aozijx.passly.feature.vault.editor.bankcard

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.CardSecret

internal object BankCardEntryFactory {

    fun create(
        state: AddBankCardFormState,
        now: Long = System.currentTimeMillis()
    ): VaultEntry = VaultEntry(
        header = EntryHeader(
            id = EntryId(""),
            entryType = EntryType.BANK_CARD,
            version = EntryVersion.INITIAL,
            createdAt = now,
            updatedAt = now
        ),
        summary = EntrySummary(
            title = state.title.trim(),
            username = state.cardholder.trim(),
            tags = state.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        ),
        secret = EntrySecret(
            card = CardSecret(
                cardType = state.cardType?.name,
                cardNumber = state.cardNumber.trim(),
                cardExpiry = buildExpiry(state.cardExpiryMonth, state.cardExpiryYear),
                cardCvv = state.cardCvv.trim().takeIf(String::isNotEmpty),
                cardHolder = state.cardholder.trim(),
                paymentPin = state.paymentPin.trim().takeIf(String::isNotEmpty),
                billingAddress = state.billingAddress.trim().takeIf(String::isNotEmpty),
                hasCardNumber = state.cardNumber.isNotBlank(),
                hasCardCvv = state.cardCvv.isNotBlank(),
                hasPaymentPin = state.paymentPin.isNotBlank()
            ),
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    )

    private fun buildExpiry(month: String, year: String): String? {
        val m = month.trim()
        val y = year.trim()
        if (m.isEmpty() && y.isEmpty()) return null
        return "$m/$y"
    }
}