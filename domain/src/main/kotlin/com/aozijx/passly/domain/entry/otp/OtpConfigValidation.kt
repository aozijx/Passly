package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpMovingFactor
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Checks usable configuration without rewriting stored secret or identity metadata. */
object OtpConfigValidation {
    fun validate(config: OtpConfig): Set<FieldKey> {
        val invalid =
            validateParameters(
                config.type,
                config.algorithm,
                config.digits,
                config.periodSeconds,
                config.counter,
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
    ): Set<FieldKey> =
        buildSet {
            val definition = OtpTypeDefinitions[type]
            if (digits == null || digits !in definition.allowedDigits) add(FieldKey.OTP_DIGITS)
            if (definition.movingFactor == OtpMovingFactor.COUNTER) {
                if (counter == null || counter < 0) add(FieldKey.OTP_COUNTER)
            } else if (period == null || period <= 0) {
                add(FieldKey.OTP_PERIOD)
            }
            if (definition.fixedPeriodSeconds != null && period != definition.fixedPeriodSeconds) {
                add(FieldKey.OTP_PERIOD)
            }
            if (definition.fixedAlgorithm != null && algorithm != definition.fixedAlgorithm) {
                add(FieldKey.OTP_ALGORITHM)
            }
        }
}
