package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError

/** Volatile OTP availability published by the app-scoped runtime; never contains secret material. */
sealed interface OtpCodeState {
    data object Pending : OtpCodeState

    sealed interface Ready : OtpCodeState {
        val code: String
        val progress: Float
    }

    data class TimedReady(
        override val code: String,
        val nextCode: String,
        override val progress: Float,
        val windowEndMillis: Long,
        val periodSeconds: Int,
    ) : Ready

    data class HotpReady(
        override val code: String,
        val counter: Long = 0L,
        val canRollbackPrevious: Boolean = false,
    ) : Ready {
        override val progress: Float = 0f
    }

    data class Failed(
        val error: OtpGenerationError,
        val canRollbackPrevious: Boolean = false,
    ) : OtpCodeState
}
