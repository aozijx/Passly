package com.aozijx.passly.feature.vault.presentation.list.model

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

@Immutable
sealed interface VaultOtpUiState {
    data object NotApplicable : VaultOtpUiState

    data object Hidden : VaultOtpUiState

    data object NotRequested : VaultOtpUiState

    data object Pending : VaultOtpUiState

    data class Ready(
        val displayCode: String,
        val progress: Float,
    ) : VaultOtpUiState

    data object Failed : VaultOtpUiState
}

val VaultOtpUiState.progress: Float
    get() = when (this) {
        is VaultOtpUiState.Ready -> progress
        VaultOtpUiState.NotApplicable,
        VaultOtpUiState.Hidden,
        VaultOtpUiState.NotRequested,
        VaultOtpUiState.Pending,
        VaultOtpUiState.Failed,
        -> 0f
    }

val VaultOtpUiState.shouldDisplayCode: Boolean
    get() = when (this) {
        VaultOtpUiState.NotApplicable,
        VaultOtpUiState.Hidden,
        -> false

        VaultOtpUiState.NotRequested,
        VaultOtpUiState.Pending,
        is VaultOtpUiState.Ready,
        VaultOtpUiState.Failed,
        -> true
    }

val VaultOtpUiState.displayCodeOrPlaceholder: String
    get() = when (this) {
        is VaultOtpUiState.Ready -> displayCode
        VaultOtpUiState.NotApplicable,
        VaultOtpUiState.Hidden,
        VaultOtpUiState.NotRequested,
        VaultOtpUiState.Pending,
        VaultOtpUiState.Failed,
        -> OTP_CODE_PLACEHOLDER
    }

private const val OTP_CODE_PLACEHOLDER = "------"
