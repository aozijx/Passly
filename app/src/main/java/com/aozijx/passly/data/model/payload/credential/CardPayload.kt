package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class CardPayload(
    val cardCvv: String? = null,
    val cardExpiration: String? = null
)