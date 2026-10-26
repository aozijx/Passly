package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** Checks usable configuration without rewriting stored secret or identity metadata. */
object OtpConfigValidation {
    fun validate(config: OtpConfig): Set<FieldKey> {
        val invalid = validateParameters(
            config.type, config.algorithm, config.digits, config.periodSeconds, config.counter,
        ).toMutableSet()
        val secret = config.secret
        if (secret == null) {
            invalid += FieldKey.OTP_SECRET
        } else {
            try {
                OtpSecretCodec.decode(secret, config.encoding).fill(0)
            } catch (_: OtpGenerationError) {
                invalid += FieldKey.OTP_SECRET
            }
        }
        return invalid.toSet()
    }

    internal fun validateParameters(
        type: OtpType,
        algorithm: OtpHashAlgorithm,
        digits: Int?,
        period: Int?,
        counter: Long?,
    ): Set<FieldKey> = buildSet {
        val validDigits = when (type) {
            OtpType.STEAM -> digits == 5
            OtpType.TOTP, OtpType.HOTP -> digits != null && digits in 6..8
        }
        if (!validDigits) add(FieldKey.OTP_DIGITS)
        when (type) {
            OtpType.HOTP -> if (counter == null || counter < 0) add(FieldKey.OTP_COUNTER)
            OtpType.TOTP -> if (period == null || period <= 0) add(FieldKey.OTP_PERIOD)
            OtpType.STEAM -> {
                if (period != 30) add(FieldKey.OTP_PERIOD)
                if (algorithm != OtpHashAlgorithm.SHA1) add(FieldKey.OTP_ALGORITHM)
            }
        }
    }
}
