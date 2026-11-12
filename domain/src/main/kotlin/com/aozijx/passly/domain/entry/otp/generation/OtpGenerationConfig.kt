package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

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
        require(digits in 5..8)
        require(periodSeconds == null || periodSeconds > 0)
        require(counter == null || counter >= 0)
        require(type != OtpType.HOTP || counter != null)
        require(type == OtpType.HOTP || periodSeconds != null)
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
