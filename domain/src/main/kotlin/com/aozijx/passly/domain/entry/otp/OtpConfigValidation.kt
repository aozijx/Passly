package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpConfigParameter
import com.aozijx.passly.domain.entry.model.otp.OtpFactorDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Checks usable configuration without rewriting stored secret or identity metadata. */
object OtpConfigValidation {
    fun validate(config: OtpConfig): Set<FieldKey> {
        val invalid =
            when (config) {
                is OtpConfig.Totp -> validateParameters(config.type, config.algorithm, config.digits, config.periodSeconds, null)
                is OtpConfig.Hotp -> validateParameters(config.type, config.algorithm, config.digits, null, config.counter)
                is OtpConfig.Steam -> validateParameters(config.type, config.algorithm, config.digits, SteamDefinition.PERIOD_SECONDS, null)
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

    fun validateParameters(
        type: OtpType,
        algorithm: OtpHashAlgorithm,
        digits: Int?,
        period: Int?,
        counter: Long?,
    ): Set<FieldKey> =
        buildSet {
            val definition = OtpTypeDefinitions[type]
            if (digits == null || digits !in definition.allowedDigits) add(FieldKey.OTP_DIGITS)
            when (val factor = definition.factor) {
                is OtpFactorDefinition.Time -> {
                    if (period == null || period <= 0) add(FieldKey.OTP_PERIOD)
                    if (counter != null) add(FieldKey.OTP_COUNTER)
                    if (OtpConfigParameter.PERIOD !in definition.editableParameters && period != factor.defaultPeriodSeconds) {
                        add(FieldKey.OTP_PERIOD)
                    }
                }

                is OtpFactorDefinition.Counter -> {
                    if (counter == null || counter < 0) add(FieldKey.OTP_COUNTER)
                    if (period != null) add(FieldKey.OTP_PERIOD)
                }
            }
            if (algorithm !in definition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }
}
