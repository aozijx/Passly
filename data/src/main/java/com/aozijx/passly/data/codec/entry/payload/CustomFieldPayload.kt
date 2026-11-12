package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
internal data class CustomFieldPayload(
    val name: String,
    val value: String,
    val type: Int = 0,
)
