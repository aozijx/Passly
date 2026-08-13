package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError

/** Volatile OTP presentation state; deliberately kept outside the domain model. */
data class OtpUiState(
    val code: String? = null,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val error: OtpGenerationError? = null,
)
