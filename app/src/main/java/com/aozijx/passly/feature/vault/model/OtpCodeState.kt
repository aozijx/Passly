package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError

/** Volatile OTP presentation state; deliberately kept outside the domain model. */
sealed interface OtpCodeState {
    data object Pending : OtpCodeState
    data class Ready(
        val code: String,
        val progress: Float,
    ) : OtpCodeState
    data class Failed(val error: OtpGenerationError) : OtpCodeState
}
