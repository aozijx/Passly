package com.aozijx.passly.feature.recovery.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

sealed interface RecoveryModeUiState {
    val dialogDraft: PasswordSetupDraft?
    val isSaving: Boolean get() = false
    val canSubmit: Boolean
    @get:StringRes val failureMessageRes: Int? get() = null

    data object Closed : RecoveryModeUiState {
        override val dialogDraft: PasswordSetupDraft? = null
        override val canSubmit = false
    }

    data class Editing(
        val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : RecoveryModeUiState {
        override val dialogDraft: PasswordSetupDraft = draft
    }

    data class Saving(
        val draft: PasswordSetupDraft,
        override val canSubmit: Boolean = false,
    ) : RecoveryModeUiState {
        override val dialogDraft: PasswordSetupDraft = draft
        override val isSaving = true
    }

    data class Failed(
        val draft: PasswordSetupDraft,
        val reason: RecoveryPasswordSetupFailure,
        override val canSubmit: Boolean = draft.canSubmit(),
    ) : RecoveryModeUiState {
        override val dialogDraft: PasswordSetupDraft = draft
        @get:StringRes override val failureMessageRes = reason.messageRes
    }
}

enum class RecoveryPasswordSetupFailure {
    PasswordMismatchOrEmpty,
    SetupFailed,
}

@get:StringRes
private val RecoveryPasswordSetupFailure.messageRes: Int
    get() =
        when (this) {
            RecoveryPasswordSetupFailure.PasswordMismatchOrEmpty -> R.string.recovery_mode_password_mismatch_or_empty
            RecoveryPasswordSetupFailure.SetupFailed -> R.string.recovery_mode_password_setup_failed
        }
