package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind

internal class CardEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.CARD

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is CardCredential) { "CARD adapter requires CardCredential" }
        return SecretPayload(card = CardCredentialPayload(
            cardType = credential.cardType,
            cardNumber = credential.cardNumber,
            cardExpiry = credential.cardExpiry,
            cardCvv = credential.cardCvv,
            cardHolder = credential.cardHolder,
            paymentPin = credential.paymentPin,
            paymentPlatform = credential.paymentPlatform,
            billingAddress = credential.billingAddress,
            issuingBank = credential.issuingBank,
            bankBranch = credential.bankBranch,
            cardProduct = credential.cardProduct,
            currency = credential.currency,
            bankPhone = credential.bankPhone,
        ))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val card = requireNotNull(payload.card) { "Card payload is missing" }
        return CardCredential(
            cardType = card.cardType,
            cardNumber = card.cardNumber,
            cardExpiry = card.cardExpiry,
            cardCvv = card.cardCvv,
            cardHolder = card.cardHolder,
            paymentPin = card.paymentPin,
            paymentPlatform = card.paymentPlatform,
            billingAddress = card.billingAddress,
            issuingBank = card.issuingBank,
            bankBranch = card.bankBranch,
            cardProduct = card.cardProduct,
            currency = card.currency,
            bankPhone = card.bankPhone,
        )
    }
}
