package com.aozijx.passly.feature.vault.presentation.list.model

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

@Immutable
sealed interface VaultOtpUiState {
    data object Pending : VaultOtpUiState

    data class Ready(
        val code: String,
        val progress: Float,
    ) : VaultOtpUiState

    data object Failed : VaultOtpUiState
}
