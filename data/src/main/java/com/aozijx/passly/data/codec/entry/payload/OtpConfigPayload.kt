package com.aozijx.passly.data.codec.entry.payload

import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import kotlinx.serialization.Serializable

@Serializable
data class OtpConfigPayload(
    val type: OtpType = OtpType.TOTP,
    /** `null` when the secret is stored as a separate field-level ciphertext. */
    val secret: String? = null,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String? = null,
    val accountName: String? = null,
)
