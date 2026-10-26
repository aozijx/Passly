package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** Unparsed user input. Empty or malformed numbers never acquire fallback values. */
data class OtpConfigInput(
    val secret: String,
    val type: OtpType = OtpType.TOTP,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: String = "6",
    val period: String = "30",
    val counter: String = "0",
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String = "",
    val accountName: String = "",
) {
    override fun toString() = "OtpConfigInput(***)"

    fun parse(): OtpConfigInputResult {
        val invalid = mutableSetOf<FieldKey>()
        val parsedDigits = digits.trim().toIntOrNull()
        val validDigits = when (type) {
            OtpType.STEAM -> parsedDigits == 5
            OtpType.TOTP, OtpType.HOTP -> parsedDigits != null && parsedDigits in 6..8
        }
        if (!validDigits) invalid += FieldKey.OTP_DIGITS
        val parsedPeriod = if (type == OtpType.HOTP) null else period.trim().toIntOrNull()
        if (type != OtpType.HOTP && (
                parsedPeriod == null || parsedPeriod <= 0 || type == OtpType.STEAM && parsedPeriod != 30
            )
        ) {
            invalid += FieldKey.OTP_PERIOD
        }
        val parsedCounter = if (type == OtpType.HOTP) counter.trim().toLongOrNull() else null
        if (type == OtpType.HOTP && (parsedCounter == null || parsedCounter < 0)) {
            invalid += FieldKey.OTP_COUNTER
        }
        if (type == OtpType.STEAM && algorithm != OtpHashAlgorithm.SHA1) invalid += FieldKey.OTP_ALGORITHM

        val normalizedSecret = try {
            when (encoding) {
                OtpSecretEncoding.BASE32 -> OtpSecretCodec.canonicalBase32(
                    secret.filterNot(Char::isWhitespace), encoding,
                )
                OtpSecretEncoding.BASE64 -> secret.trim().also { OtpSecretCodec.decode(it, encoding).fill(0) }
            }
        } catch (_: OtpGenerationError) {
            return OtpConfigInputResult.Invalid(invalid + FieldKey.OTP_SECRET)
        }
        if (invalid.isNotEmpty()) return OtpConfigInputResult.Invalid(invalid.toSet())
        return OtpConfigInputResult.Valid(OtpConfig(
            type = type,
            secret = normalizedSecret,
            algorithm = algorithm,
            digits = requireNotNull(parsedDigits),
            periodSeconds = parsedPeriod,
            counter = parsedCounter,
            encoding = encoding,
            issuer = issuer.trim().takeIf(String::isNotEmpty),
            accountName = accountName.trim().takeIf(String::isNotEmpty),
        ))
    }
}

sealed interface OtpConfigInputResult {
    data class Valid(val config: OtpConfig) : OtpConfigInputResult {
        override fun toString() = "Valid(***)"
    }
    data class Invalid(val fields: Set<FieldKey>) : OtpConfigInputResult
}
