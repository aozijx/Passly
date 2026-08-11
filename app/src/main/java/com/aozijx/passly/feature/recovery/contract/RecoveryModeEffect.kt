package com.aozijx.passly.feature.recovery.contract

sealed interface RecoveryModeEffect {
    data object ExitRecovery : RecoveryModeEffect
    data object PasswordResetCompleted : RecoveryModeEffect
}
