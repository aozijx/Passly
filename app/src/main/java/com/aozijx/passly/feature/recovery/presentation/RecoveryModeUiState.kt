package com.aozijx.passly.feature.recovery.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

sealed interface RecoveryModeUiState {
    data object Closed : RecoveryModeUiState

    sealed interface PasswordDialog : RecoveryModeUiState {
        val draft: PasswordSetupDraft
        val canSubmit: Boolean
        val isSaving: Boolean get() = false
        val error: RecoveryPasswordDialogError get() = RecoveryPasswordDialogError.None
    }

    data class Editing(
        override val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : PasswordDialog

    data class Saving(
        override val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = false,
    ) : PasswordDialog {
        override val isSaving = true
    }

    data class Failed(
        override val draft: PasswordSetupDraft,
        val reason: RecoveryPasswordSetupFailure,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : PasswordDialog {
        override val error = RecoveryPasswordDialogError.Message(reason.messageRes)
    }
}

sealed interface RecoveryPasswordDialogError {
    data object None : RecoveryPasswordDialogError

    data class Message(
        @get:StringRes val messageRes: Int,
    ) : RecoveryPasswordDialogError
}

enum class RecoveryPasswordSetupFailure(
    @get:StringRes val messageRes: Int,
) {
    PasswordMismatchOrEmpty(R.string.recovery_mode_password_mismatch_or_empty),
    SetupFailed(R.string.recovery_mode_password_setup_failed),
}
