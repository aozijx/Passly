package com.aozijx.passly.presentation.feature.recovery

sealed interface RecoveryModeEffect {
    data object ExitRecovery : RecoveryModeEffect
    data object PasswordResetCompleted : RecoveryModeEffect
}
