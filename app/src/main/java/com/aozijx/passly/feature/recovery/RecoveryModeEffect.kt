package com.aozijx.passly.feature.recovery

sealed interface RecoveryModeEffect {
    data object ExitRecovery : RecoveryModeEffect
    data object PasswordResetCompleted : RecoveryModeEffect
}
