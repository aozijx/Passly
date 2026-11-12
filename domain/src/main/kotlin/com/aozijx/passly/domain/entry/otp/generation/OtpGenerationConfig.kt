package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpMovingFactor
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions

/** Non-sensitive OTP parameters required by the generation algorithm. */
data class OtpGenerationConfig(
    val type: OtpType,
    val algorithm: OtpHashAlgorithm,
    val digits: Int,
    val periodSeconds: Int?,
    val counter: Long?,
    val encoding: OtpSecretEncoding,
) {
    init {
        val definition = OtpTypeDefinitions[type]
        require(digits in definition.allowedDigits)
        require(periodSeconds == null || periodSeconds > 0)
        require(counter == null || counter >= 0)
        require(definition.movingFactor == OtpMovingFactor.COUNTER || periodSeconds != null)
        require(definition.movingFactor == OtpMovingFactor.TIME || counter != null)
        require(definition.fixedPeriodSeconds == null || periodSeconds == definition.fixedPeriodSeconds)
        require(definition.fixedAlgorithm == null || algorithm == definition.fixedAlgorithm)
    }
}

fun OtpConfig.toGenerationConfig(): OtpGenerationConfig =
    OtpGenerationConfig(
        type = type,
        algorithm = algorithm,
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        encoding = encoding,
    )
