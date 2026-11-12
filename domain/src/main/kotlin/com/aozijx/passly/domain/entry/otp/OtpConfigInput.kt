package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpFactorDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Unparsed user input. Empty or malformed numbers never acquire fallback values. */
data class OtpConfigInput(
    val secret: String,
    val type: OtpType = OtpType.TOTP,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: String = TotpDefinition.DEFAULT_DIGITS.toString(),
    val period: String = TotpDefinition.DEFAULT_PERIOD_SECONDS.toString(),
    val counter: String = HotpDefinition.INITIAL_COUNTER.toString(),
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String = "",
    val accountName: String = "",
) {
    override fun toString() = "OtpConfigInput(***)"

    fun parse(): OtpConfigInputResult {
        val parsedDigits = digits.trim().toIntOrNull()
        val definition = OtpTypeDefinitions[type]
        val parsedPeriod = (definition.factor as? OtpFactorDefinition.Time)?.let { period.trim().toIntOrNull() }
        val parsedCounter = (definition.factor as? OtpFactorDefinition.Counter)?.let { counter.trim().toLongOrNull() }
        val invalid = OtpConfigValidation.validateParameters(type, algorithm, parsedDigits, parsedPeriod, parsedCounter)

        val normalizedSecret =
            try {
                when (encoding) {
                    OtpSecretEncoding.BASE32 -> {
                        OtpSecretCodec.canonicalBase32(
                            secret.filterNot(Char::isWhitespace),
                            encoding,
                        )
                    }

                    OtpSecretEncoding.BASE64 -> {
                        secret.trim().also { OtpSecretCodec.decode(it, encoding).fill(0) }
                    }
                }
            } catch (_: OtpGenerationError) {
                return OtpConfigInputResult.Invalid(invalid + FieldKey.OTP_SECRET)
            }
        if (invalid.isNotEmpty()) return OtpConfigInputResult.Invalid(invalid)
        val normalizedIssuer = issuer.trim().takeIf(String::isNotEmpty)
        val normalizedAccountName = accountName.trim().takeIf(String::isNotEmpty)
        val config =
            when (type) {
                OtpType.TOTP ->
                    OtpConfig.Totp(
                        secret = normalizedSecret,
                        algorithm = algorithm,
                        digits = requireNotNull(parsedDigits),
                        periodSeconds = requireNotNull(parsedPeriod),
                        encoding = encoding,
                        issuer = normalizedIssuer,
                        accountName = normalizedAccountName,
                    )

                OtpType.HOTP ->
                    OtpConfig.Hotp(
                        secret = normalizedSecret,
                        algorithm = algorithm,
                        digits = requireNotNull(parsedDigits),
                        counter = requireNotNull(parsedCounter),
                        encoding = encoding,
                        issuer = normalizedIssuer,
                        accountName = normalizedAccountName,
                    )

                OtpType.STEAM ->
                    OtpConfig.Steam(
                        secret = normalizedSecret,
                        encoding = encoding,
                        issuer = normalizedIssuer,
                        accountName = normalizedAccountName,
                    )
            }
        return OtpConfigInputResult.Valid(config)
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
