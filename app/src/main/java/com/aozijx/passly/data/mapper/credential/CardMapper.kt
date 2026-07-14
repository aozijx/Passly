package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.CardPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toCardPayload(): CardPayload? {
    if (cardCvv.isNullOrBlank() && cardExpiration.isNullOrBlank()) {
        return null
    }
    return CardPayload(
        cardCvv = cardCvv,
        cardExpiration = cardExpiration
    )
}

fun VaultEntry.mergeCard(payload: CardPayload?): VaultEntry {
    payload ?: return this
    return copy(
        cardCvv = payload.cardCvv,
        cardExpiration = payload.cardExpiration
    )
}