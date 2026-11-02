package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class OtpConfigPayload(
    val type: String = "TOTP",
    /** `null` when the secret is stored as a separate field-level ciphertext. */
    val secret: String? = null,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: String = "BASE32",
    val issuer: String? = null,
    val accountName: String? = null,
)
