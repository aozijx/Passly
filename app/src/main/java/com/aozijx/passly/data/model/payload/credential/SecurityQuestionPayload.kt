package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class SecurityQuestionPayload(
    val question: String? = null,
    val answer: String? = null
)