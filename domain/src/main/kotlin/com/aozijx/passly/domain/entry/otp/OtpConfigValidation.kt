package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec

/** Checks user supplied OTP parameters and encoded secret material without rewriting the input. */
object OtpConfigValidation {
    fun isSecretValid(
        secret: String?,
        encoding: OtpSecretEncoding,
    ): Boolean {
        if (secret == null) return false
        return try {
            OtpSecretCodec.decode(secret, encoding).fill(0)
            true
        } catch (_: OtpGenerationError) {
            false
        }
    }

    fun validateTotp(
        algorithm: OtpHashAlgorithm,
        digits: Int?,
        periodSeconds: Int?,
    ): Set<FieldKey> =
        buildSet {
            if (digits == null || digits !in TotpDefinition.allowedDigits) add(FieldKey.OTP_DIGITS)
            if (periodSeconds == null || periodSeconds <= 0) add(FieldKey.OTP_PERIOD)
            if (algorithm !in TotpDefinition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }

    fun validateHotp(
        algorithm: OtpHashAlgorithm,
        digits: Int?,
        counter: Long?,
    ): Set<FieldKey> =
        buildSet {
            if (digits == null || digits !in HotpDefinition.allowedDigits) add(FieldKey.OTP_DIGITS)
            if (counter == null || counter < 0L) add(FieldKey.OTP_COUNTER)
            if (algorithm !in HotpDefinition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }

    fun validateSteam(
        algorithm: OtpHashAlgorithm,
        digits: Int,
        periodSeconds: Int,
    ): Set<FieldKey> =
        buildSet {
            if (digits != SteamDefinition.DIGITS) add(FieldKey.OTP_DIGITS)
            if (periodSeconds != SteamDefinition.PERIOD_SECONDS) add(FieldKey.OTP_PERIOD)
            if (algorithm !in SteamDefinition.supportedAlgorithms) add(FieldKey.OTP_ALGORITHM)
        }
}
