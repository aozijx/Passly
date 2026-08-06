package com.aozijx.passly.feature.recovery.contract

sealed interface RecoveryModeEffect {
    data class PickExportTarget(val fileName: String) : RecoveryModeEffect
    data object ExitRecovery : RecoveryModeEffect
    data object PasswordResetCompleted : RecoveryModeEffect
    data object ExportCompleted : RecoveryModeEffect
}
