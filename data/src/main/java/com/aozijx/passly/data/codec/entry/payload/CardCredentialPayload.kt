package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class CardCredentialPayload(
    val cardType: String? = null,
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val billingAddress: String? = null,
    val issuingBank: String? = null,
    val bankBranch: String? = null,
    val cardProduct: String? = null,
    val currency: String? = null,
    val bankPhone: String? = null,
)
