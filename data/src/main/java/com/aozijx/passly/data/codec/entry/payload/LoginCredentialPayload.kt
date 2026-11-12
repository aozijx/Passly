package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginCredentialPayload(
    val email: String? = null,
    val password: String? = null,
)
