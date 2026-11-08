package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

sealed interface RecoveryModeUiState {
    val canSubmit: Boolean

    data object Closed : RecoveryModeUiState {
        override val canSubmit = false
    }

    data class Editing(
        val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : RecoveryModeUiState

    data class Saving(
        val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = false,
    ) : RecoveryModeUiState

    data class Failed(
        val draft: PasswordSetupDraft,
        val reason: RecoveryPasswordSetupFailure,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : RecoveryModeUiState
}

enum class RecoveryPasswordSetupFailure {
    PasswordMismatchOrEmpty,
    SetupFailed,
}
