package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.PaymentPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toPaymentPayload(): PaymentPayload? {
    if (paymentPin.isNullOrBlank() && paymentPlatform.isNullOrBlank()) {
        return null
    }
    return PaymentPayload(
        pin = paymentPin,
        platform = paymentPlatform
    )
}

fun VaultEntry.mergePayment(payload: PaymentPayload?): VaultEntry {
    payload ?: return this
    return copy(
        paymentPin = payload.pin,
        paymentPlatform = payload.platform
    )
}