package com.aozijx.passly.domain.entry.model.otp


enum class OtpType {
    TOTP,
    HOTP,
    STEAM,
}

enum class OtpMovingFactor {
    TIME,
    COUNTER,
}

enum class OtpOutputFormat {
    DECIMAL,
    STEAM_ALPHABET,
}

enum class OtpAuthUriKind(val schemeAuthority: String) {
    TIME_BASED("totp"),
    COUNTER_BASED("hotp"),
}

enum class OtpHashAlgorithm {
    SHA1,
    SHA256,
    SHA512,
}

enum class OtpSecretEncoding {
    BASE32,
    BASE64,
}

data class OtpConfig(
    val type: OtpType = OtpType.TOTP,
    /**
     * `null` means the secret is stored as a separate field-level ciphertext and has not been
     * decrypted yet. Callers that generate codes must obtain the value first (field reveal or
     * complete read).
     */
    val secret: String? = null,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String? = null,
    val accountName: String? = null,
) {
    init {
        require(secret == null || secret.isNotBlank()) { "OTP secret cannot be blank" }
        val definition = OtpTypeDefinitions[type]
        require(digits in definition.allowedDigits) { "OTP digits are not supported by $type" }
        require(periodSeconds == null || periodSeconds > 0) { "OTP period must be positive" }
        require(counter == null || counter >= 0) { "OTP counter cannot be negative" }
        require(definition.movingFactor == OtpMovingFactor.COUNTER || periodSeconds != null) {
            "Time-based OTP requires a period"
        }
        require(definition.movingFactor == OtpMovingFactor.TIME || counter != null) {
            "Counter-based OTP requires a counter"
        }
        require(definition.fixedPeriodSeconds == null || periodSeconds == definition.fixedPeriodSeconds) {
            "OTP type $type requires a fixed period"
        }
        require(definition.fixedAlgorithm == null || algorithm == definition.fixedAlgorithm) {
            "OTP type $type requires a fixed algorithm"
        }
    }

    companion object {
        fun steamDefaults(
            secret: String,
            issuer: String? = null,
        ): OtpConfig =
            OtpConfig(
                type = OtpType.STEAM,
                secret = secret,
                algorithm = OtpHashAlgorithm.SHA1,
                digits = 5,
                periodSeconds = 30,
                encoding = OtpSecretEncoding.BASE32,
                issuer = issuer,
            )
    }
}
