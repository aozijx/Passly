package com.aozijx.passly.feature.recovery.contract

sealed interface RecoveryModeEffect {
    data class PickExportTarget(val fileName: String) : RecoveryModeEffect
    data object ExitRecovery : RecoveryModeEffect
    data class ShowMessage(val message: String) : RecoveryModeEffect
}