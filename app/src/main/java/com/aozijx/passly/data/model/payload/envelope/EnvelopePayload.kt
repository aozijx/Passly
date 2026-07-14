package com.aozijx.passly.data.model.payload.envelope

import kotlinx.serialization.Serializable

@Serializable
data class EnvelopePayload(
    val envelopeId: String,
    val type: Int,
    val algorithm: Int,
    val createdAt: Long,
    val updatedAt: Long
)