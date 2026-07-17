package com.aozijx.passly.domain.model.credential

import kotlinx.serialization.Serializable

@Serializable
data class CustomField(
    val name: String,
    val value: String,
    val type: Int = 0
)