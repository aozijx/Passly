package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class CustomFieldPayload(
    val name: String,
    val value: String,
    val type: Int = 0,
)
