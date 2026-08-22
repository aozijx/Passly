package com.aozijx.passly.feature.vault.editor.bankcard

import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardFormState
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.github.f4b6a3.uuid.UuidCreator

internal object BankCardEntryFactory {

    fun create(
        state: AddBankCardFormState,
        now: Long = System.currentTimeMillis()
    ): Entry = Entry(
        identity = EntryIdentity(
            id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
            type = EntryType.BANK_CARD,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(now),
        ),
        profile = EntryProfile(
            title = state.title.trim(),
            username = state.cardholder.trim(),
            tags = state.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }.toSet()
        ),
        secret = EntrySecret(
            credential = CardCredential(
                cardType = state.cardType?.name,
                cardNumber = state.cardNumber.trim(),
                cardExpiry = buildExpiry(state.cardExpiryMonth, state.cardExpiryYear),
                cardCvv = state.cardCvv.trim().takeIf(String::isNotEmpty),
                cardHolder = state.cardholder.trim(),
                paymentPin = state.paymentPin.trim().takeIf(String::isNotEmpty),
                billingAddress = state.billingAddress.trim().takeIf(String::isNotEmpty),
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
