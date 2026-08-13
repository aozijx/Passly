package com.aozijx.passly.feature.recovery.contract

sealed interface RecoveryModeIntent {
    data object SetPasswordClicked : RecoveryModeIntent
    data class NewPasswordChanged(val value: String) : RecoveryModeIntent
    data class ConfirmPasswordChanged(val value: String) : RecoveryModeIntent
    data object SubmitNewPassword : RecoveryModeIntent

    data object ExitClicked : RecoveryModeIntent
    data object DismissPasswordDialog : RecoveryModeIntent
}
