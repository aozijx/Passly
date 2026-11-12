package com.aozijx.passly.domain.entry.model.otp

enum class OtpType {
    TOTP,
    HOTP,
    STEAM,
}

enum class OtpOutputFormat {
    DECIMAL,
    STEAM_ALPHABET,
}

enum class OtpAuthUriKind(
    val schemeAuthority: String,
) {
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

/** Steam uses the TOTP time factor with its own output alphabet and fixed parameters. */
enum class TotpVariant {
    STANDARD,
    STEAM,
}

/** Configuration variants contain only the parameters their OTP algorithm consumes. */
sealed interface OtpConfig {
    val type: OtpType

    /** Null means the secret remains in separate field-level encrypted storage. */
    val secret: String?
    val encoding: OtpSecretEncoding
    val issuer: String?
    val accountName: String?

    data class Totp(
        override val secret: String? = null,
        val algorithm: OtpHashAlgorithm = TotpDefinition.defaultAlgorithm,
        val digits: Int = TotpDefinition.DEFAULT_DIGITS,
        val periodSeconds: Int = TotpDefinition.DEFAULT_PERIOD_SECONDS,
        override val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
        override val issuer: String? = null,
        override val accountName: String? = null,
        val variant: TotpVariant = TotpVariant.STANDARD,
    ) : OtpConfig {
        override val type =
            when (variant) {
                TotpVariant.STANDARD -> OtpType.TOTP
                TotpVariant.STEAM -> OtpType.STEAM
            }

        init {
            require(secret == null || secret.isNotBlank()) { "OTP secret cannot be blank" }
            when (variant) {
                TotpVariant.STANDARD -> {
                    require(digits in TotpDefinition.allowedDigits) { "TOTP digits are unsupported" }
                    require(periodSeconds > 0) { "TOTP period must be positive" }
                    require(algorithm in TotpDefinition.supportedAlgorithms) { "TOTP algorithm is unsupported" }
                }

                TotpVariant.STEAM -> {
                    require(algorithm == SteamDefinition.defaultAlgorithm) { "Steam algorithm is fixed" }
                    require(digits == SteamDefinition.DIGITS) { "Steam digits are fixed" }
                    require(periodSeconds == SteamDefinition.PERIOD_SECONDS) { "Steam period is fixed" }
                }
            }
        }
    }

    data class Hotp(
        override val secret: String? = null,
        val algorithm: OtpHashAlgorithm = HotpDefinition.defaultAlgorithm,
        val digits: Int = HotpDefinition.DEFAULT_DIGITS,
        val counter: Long = HotpDefinition.INITIAL_COUNTER,
        override val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
        override val issuer: String? = null,
        override val accountName: String? = null,
    ) : OtpConfig {
        override val type = OtpType.HOTP

        init {
            require(secret == null || secret.isNotBlank()) { "OTP secret cannot be blank" }
            require(digits in HotpDefinition.allowedDigits) { "HOTP digits are unsupported" }
            require(counter >= 0L) { "HOTP counter cannot be negative" }
        }
    }
}

/** Returns the same algorithm-specific configuration with a replaced secret value. */
fun OtpConfig.withSecret(secret: String?): OtpConfig =
    when (this) {
        is OtpConfig.Totp -> copy(secret = secret)
        is OtpConfig.Hotp -> copy(secret = secret)
    }
