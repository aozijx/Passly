package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpOutputFormat
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.TotpVariant

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
        val variant: TotpVariant = TotpVariant.STANDARD,
    ) : Timed {
        override val outputFormat =
            when (variant) {
                TotpVariant.STANDARD -> OtpOutputFormat.DECIMAL
                TotpVariant.STEAM -> OtpOutputFormat.STEAM_ALPHABET
            }
    }

    data class Hotp(
        override val algorithm: OtpHashAlgorithm,
        override val digits: Int,
        val counter: Long,
        override val encoding: OtpSecretEncoding,
    ) : OtpGenerationConfig
}

fun OtpConfig.toGenerationConfig(): OtpGenerationConfig =
    when (this) {
        is OtpConfig.Totp -> {
            OtpGenerationConfig.Totp(
                algorithm = algorithm,
                digits = digits,
                periodSeconds = periodSeconds,
                encoding = encoding,
                variant = variant,
            )
        }

        is OtpConfig.Hotp -> {
            OtpGenerationConfig.Hotp(algorithm, digits, counter, encoding)
        }
    }
