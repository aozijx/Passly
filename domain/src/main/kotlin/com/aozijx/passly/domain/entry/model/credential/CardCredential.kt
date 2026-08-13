package com.aozijx.passly.domain.entry.model.credential


data class CardCredential(
    val cardType: String? = null,
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val billingAddress: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.CARD
}
