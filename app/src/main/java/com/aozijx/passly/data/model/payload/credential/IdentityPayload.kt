package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class IdentityPayload(
    val idNumber: String? = null
)