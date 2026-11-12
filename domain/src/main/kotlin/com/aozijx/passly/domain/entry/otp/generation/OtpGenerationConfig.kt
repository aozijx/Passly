package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpOutputFormat
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition

/** Non-sensitive, type-specific parameters required by each OTP generation algorithm. */
sealed interface OtpGenerationConfig {
    val algorithm: OtpHashAlgorithm
    val digits: Int
    val encoding: OtpSecretEncoding
    val outputFormat: OtpOutputFormat get() = OtpOutputFormat.DECIMAL

    sealed interface Timed : OtpGenerationConfig {
        val periodSeconds: Int
    }

    data class Totp(
        override val algorithm: OtpHashAlgorithm,
        override val digits: Int,
        override val periodSeconds: Int,
        override val encoding: OtpSecretEncoding,
    ) : Timed

    data class Hotp(
        override val algorithm: OtpHashAlgorithm,
        override val digits: Int,
        val counter: Long,
        override val encoding: OtpSecretEncoding,
    ) : OtpGenerationConfig

    data class Steam(
        override val encoding: OtpSecretEncoding,
    ) : Timed {
        override val algorithm = SteamDefinition.defaultAlgorithm
        override val digits = SteamDefinition.DIGITS
        override val periodSeconds = SteamDefinition.PERIOD_SECONDS
        override val outputFormat = OtpOutputFormat.STEAM_ALPHABET
    }
}

fun OtpConfig.toGenerationConfig(): OtpGenerationConfig =
    when (this) {
        is OtpConfig.Totp -> OtpGenerationConfig.Totp(algorithm, digits, periodSeconds, encoding)
        is OtpConfig.Hotp -> OtpGenerationConfig.Hotp(algorithm, digits, counter, encoding)
        is OtpConfig.Steam -> OtpGenerationConfig.Steam(encoding)
    }
