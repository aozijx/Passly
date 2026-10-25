package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class RecoveryModeUiState(
    val passwordSetup: RecoveryPasswordSetup = RecoveryPasswordSetup.Closed,
)

sealed interface RecoveryPasswordSetup {
    data object Closed : RecoveryPasswordSetup
    data class Editing(
        val newPassword: SensitiveValue = EmptySensitiveValue,
        val confirmPassword: SensitiveValue = EmptySensitiveValue,
        val phase: RecoveryPasswordSetupPhase = RecoveryPasswordSetupPhase.Idle,
    ) : RecoveryPasswordSetup
}

sealed interface RecoveryPasswordSetupPhase {
    data object Idle : RecoveryPasswordSetupPhase
    data object Saving : RecoveryPasswordSetupPhase
    data class Failed(val reason: RecoveryPasswordSetupFailure) : RecoveryPasswordSetupPhase
}

enum class RecoveryPasswordSetupFailure {
    PasswordMismatchOrEmpty,
    SetupFailed,
}
