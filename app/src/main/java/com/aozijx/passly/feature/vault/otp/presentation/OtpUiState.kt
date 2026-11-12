package com.aozijx.passly.feature.vault.otp.presentation

import androidx.compose.runtime.Immutable

/** Complete render state shared by Vault list and entry detail OTP surfaces. */
@Immutable
sealed interface OtpUiState {
    data object NotApplicable : OtpUiState

    data object Hidden : OtpUiState

    data object NotRequested : OtpUiState

    data object Pending : OtpUiState

    data class TimedReady(
        val currentCode: String,
        val nextCode: String,
        val windowEndMillis: Long,
        val periodSeconds: Int,
    ) : OtpUiState

    data class HotpReady(
        val currentCode: String,
        val counter: Long = 0L,
        val canRollbackPrevious: Boolean = false,
    ) : OtpUiState

    data class Failed(
        val canRollbackPrevious: Boolean = false,
    ) : OtpUiState
}

val OtpUiState.canRollbackPrevious: Boolean
    get() =
        when (this) {
            is OtpUiState.HotpReady -> canRollbackPrevious

            is OtpUiState.Failed -> canRollbackPrevious

            OtpUiState.NotApplicable,
            OtpUiState.Hidden,
            OtpUiState.NotRequested,
            OtpUiState.Pending,
            is OtpUiState.TimedReady,
            -> false
        }

val OtpUiState.displayCodeOrPlaceholder: String
    get() =
        when (this) {
            is OtpUiState.TimedReady -> currentCode

            is OtpUiState.HotpReady -> currentCode

            OtpUiState.NotApplicable,
            OtpUiState.Hidden,
            OtpUiState.NotRequested,
            OtpUiState.Pending,
            is OtpUiState.Failed,
            -> OTP_CODE_PLACEHOLDER
        }

val OtpUiState.shouldDisplayCode: Boolean
    get() = this != OtpUiState.NotApplicable && this != OtpUiState.Hidden

private const val OTP_CODE_PLACEHOLDER = "------"
