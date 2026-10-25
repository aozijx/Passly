package com.aozijx.passly.feature.recovery.presentation

sealed interface RecoveryModeEffect {
    data object ExitRecovery : RecoveryModeEffect
    data object PasswordResetCompleted : RecoveryModeEffect
}
