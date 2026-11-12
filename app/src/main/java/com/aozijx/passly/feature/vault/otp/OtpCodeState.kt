package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError

/** Volatile OTP availability published by the app-scoped runtime; never contains secret material. */
sealed interface OtpCodeState {
    data object Pending : OtpCodeState

    data class TimedReady(
        val code: String,
        val nextCode: String,
        val windowEndMillis: Long,
        val periodSeconds: Int,
    ) : OtpCodeState

    data class HotpReady(
        val code: String,
        val counter: Long = 0L,
        val canRollbackPrevious: Boolean = false,
    ) : OtpCodeState

    data class Failed(
        val error: OtpGenerationError,
        val canRollbackPrevious: Boolean = false,
    ) : OtpCodeState
}
