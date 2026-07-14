package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class PaymentPayload(
    val pin: String? = null,
    val platform: String? = null
)