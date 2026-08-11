package com.aozijx.passly.domain.entry.model.otp

data class OtpConfig(
    val type: OtpType = OtpType.TOTP,
    val secret: String,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String? = null,
    val accountName: String? = null
) {
    companion object {
        fun steamDefaults(secret: String, issuer: String? = null): OtpConfig = OtpConfig(
            type = OtpType.STEAM,
            secret = secret,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 5,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32,
            issuer = issuer
        )
    }
}
