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
    init {
        require(secret.isNotBlank()) { "OTP secret cannot be blank" }
        require(digits in 5..8) { "OTP digits must be between 5 and 8" }
        require(periodSeconds == null || periodSeconds > 0) { "OTP period must be positive" }
        require(counter == null || counter >= 0) { "OTP counter cannot be negative" }
        require(type != OtpType.HOTP || counter != null) { "HOTP requires a counter" }
        require(type == OtpType.HOTP || periodSeconds != null) { "Time-based OTP requires a period" }
    }

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
