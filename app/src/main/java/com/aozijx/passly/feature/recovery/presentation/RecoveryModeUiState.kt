package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

data class RecoveryModeUiState(
    val passwordSetup: RecoveryPasswordSetup = RecoveryPasswordSetup.Closed,
)

sealed interface RecoveryPasswordSetup {
    data object Closed : RecoveryPasswordSetup

    data class Editing(val draft: PasswordSetupDraft) : RecoveryPasswordSetup

    data class Saving(val draft: PasswordSetupDraft) : RecoveryPasswordSetup

    data class Failed(
        val draft: PasswordSetupDraft,
        val reason: RecoveryPasswordSetupFailure,
    ) : RecoveryPasswordSetup
}

enum class RecoveryPasswordSetupFailure {
    PasswordMismatchOrEmpty,
    SetupFailed,
}
