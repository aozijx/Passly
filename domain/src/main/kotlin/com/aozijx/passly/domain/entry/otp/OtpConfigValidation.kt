package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Checks usable configuration without rewriting stored secret or identity metadata. */
object OtpConfigValidation {
    fun validate(config: OtpConfig): Set<FieldKey> {
        val invalid =
            when (config) {
                is OtpConfig.Totp -> validateTotp(config.algorithm, config.digits, config.periodSeconds)
                is OtpConfig.Hotp -> validateHotp(config.algorithm, config.digits, config.counter)
                is OtpConfig.Steam -> emptySet()
            }.toMutableSet()
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

    fun validateTotp(
        algorithm: OtpHashAlgorithm,
        digits: Int,
        periodSeconds: Int,
    ): Set<FieldKey> =
        buildSet {
            if (digits !in TotpDefinition.allowedDigits) add(FieldKey.OTP_DIGITS)
            if (periodSeconds <= 0) add(FieldKey.OTP_PERIOD)
            if (algorithm !in TotpDefinition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }

    fun validateHotp(
        algorithm: OtpHashAlgorithm,
        digits: Int,
        counter: Long,
    ): Set<FieldKey> =
        buildSet {
            if (digits !in HotpDefinition.allowedDigits) add(FieldKey.OTP_DIGITS)
            if (counter < 0L) add(FieldKey.OTP_COUNTER)
            if (algorithm !in HotpDefinition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }
}
