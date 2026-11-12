package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Raw inputs are algorithm-specific, so an irrelevant period or counter cannot be carried. */
sealed interface OtpConfigInput {
    val secret: String
    val encoding: OtpSecretEncoding
    val issuer: String
    val accountName: String

    fun parse(): OtpConfigInputResult =
        when (this) {
            is Totp -> parseTotp()
            is Hotp -> parseHotp()
            is Steam -> parseSteam()
        }

    data class Totp(
        override val secret: String,
        val algorithm: OtpHashAlgorithm,
        val digits: String,
        val periodSeconds: String,
        override val encoding: OtpSecretEncoding,
        override val issuer: String,
        override val accountName: String,
    ) : OtpConfigInput {
        override fun toString() = "Totp(***)"
    }

    data class Hotp(
        override val secret: String,
        val algorithm: OtpHashAlgorithm,
        val digits: String,
        val counter: String,
        override val encoding: OtpSecretEncoding,
        override val issuer: String,
        override val accountName: String,
    ) : OtpConfigInput {
        override fun toString() = "Hotp(***)"
    }

    data class Steam(
        override val secret: String,
        override val encoding: OtpSecretEncoding,
        override val issuer: String,
        override val accountName: String,
    ) : OtpConfigInput {
        override fun toString() = "Steam(***)"
    }
}

sealed interface OtpConfigInputResult {
    data class Valid(
        val config: OtpConfig,
    ) : OtpConfigInputResult {
        override fun toString() = "Valid(***)"
    }

    data class Invalid(
        val fields: Set<FieldKey>,
    ) : OtpConfigInputResult
}

private fun OtpConfigInput.Totp.parseTotp(): OtpConfigInputResult {
    val parsedDigits = digits.trim().toIntOrNull()
    val parsedPeriod = periodSeconds.trim().toIntOrNull()
    val invalid =
        buildSet {
            addAll(OtpConfigValidation.validateTotp(algorithm, parsedDigits, parsedPeriod))
        }
    val normalizedSecret = secret.normalizeSecret(encoding) ?: return OtpConfigInputResult.Invalid(invalid + FieldKey.OTP_SECRET)
    if (invalid.isNotEmpty()) return OtpConfigInputResult.Invalid(invalid)
    return OtpConfigInputResult.Valid(
        OtpConfig.Totp(
            secret = normalizedSecret,
            algorithm = algorithm,
            digits = requireNotNull(parsedDigits),
            periodSeconds = requireNotNull(parsedPeriod),
            encoding = encoding,
            issuer = issuer.normalizedMetadata(),
            accountName = accountName.normalizedMetadata(),
        ),
    )
}

private fun OtpConfigInput.Hotp.parseHotp(): OtpConfigInputResult {
    val parsedDigits = digits.trim().toIntOrNull()
    val parsedCounter = counter.trim().toLongOrNull()
    val invalid =
        buildSet {
            addAll(OtpConfigValidation.validateHotp(algorithm, parsedDigits, parsedCounter))
        }
    val normalizedSecret = secret.normalizeSecret(encoding) ?: return OtpConfigInputResult.Invalid(invalid + FieldKey.OTP_SECRET)
    if (invalid.isNotEmpty()) return OtpConfigInputResult.Invalid(invalid)
    return OtpConfigInputResult.Valid(
        OtpConfig.Hotp(
            secret = normalizedSecret,
            algorithm = algorithm,
            digits = requireNotNull(parsedDigits),
            counter = requireNotNull(parsedCounter),
            encoding = encoding,
            issuer = issuer.normalizedMetadata(),
            accountName = accountName.normalizedMetadata(),
        ),
    )
}

private fun OtpConfigInput.Steam.parseSteam(): OtpConfigInputResult {
    val normalizedSecret = secret.normalizeSecret(encoding) ?: return OtpConfigInputResult.Invalid(setOf(FieldKey.OTP_SECRET))
    return OtpConfigInputResult.Valid(
        SteamDefinition.configuration(
            secret = normalizedSecret,
            encoding = encoding,
            issuer = issuer.normalizedMetadata(),
            accountName = accountName.normalizedMetadata(),
        ),
    )
}

private fun String.normalizeSecret(encoding: OtpSecretEncoding): String? =
    try {
        when (encoding) {
            OtpSecretEncoding.BASE32 -> OtpSecretCodec.canonicalBase32(filterNot(Char::isWhitespace), encoding)
            OtpSecretEncoding.BASE64 -> trim().also { OtpSecretCodec.decode(it, encoding).fill(0) }
        }
    } catch (_: OtpGenerationError) {
        null
    }

private fun String.normalizedMetadata(): String? = trim().takeIf(String::isNotEmpty)
