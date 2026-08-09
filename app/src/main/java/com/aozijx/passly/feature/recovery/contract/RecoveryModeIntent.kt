package com.aozijx.passly.feature.recovery.contract

sealed interface RecoveryModeIntent {
    data object SetPasswordClicked : RecoveryModeIntent
    data class NewPasswordChanged(val value: String) : RecoveryModeIntent
    data class ConfirmPasswordChanged(val value: String) : RecoveryModeIntent
    data object SubmitNewPassword : RecoveryModeIntent

    data object ReconfigureBiometricClicked : RecoveryModeIntent

    data object ExportClicked : RecoveryModeIntent
    data class ExportPasswordChanged(val value: String) : RecoveryModeIntent
    data class IncludeIconsChanged(val include: Boolean) : RecoveryModeIntent
    data class IncludeAttachmentsChanged(val include: Boolean) : RecoveryModeIntent
    data class IncludeDeletedChanged(val include: Boolean) : RecoveryModeIntent
    data object SubmitExport : RecoveryModeIntent
    data class ExportTargetPicked(val target: DocumentRef?) : RecoveryModeIntent

    data object ExitClicked : RecoveryModeIntent
    data object DismissSheet : RecoveryModeIntent
}