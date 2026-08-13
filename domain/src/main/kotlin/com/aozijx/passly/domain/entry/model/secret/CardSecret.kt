package com.aozijx.passly.domain.entry.model.secret

data class CardSecret(
    val cardType: String? = null,
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val billingAddress: String? = null,
    val hasCardNumber: Boolean = false,
    val hasCardCvv: Boolean = false,
    val hasPaymentPin: Boolean = false
)
