package com.aozijx.passly.presentation.feature.recovery

sealed interface RecoveryModeUiAction {
    data object SetPasswordClicked : RecoveryModeUiAction
    data class NewPasswordChanged(val value: String) : RecoveryModeUiAction
    data class ConfirmPasswordChanged(val value: String) : RecoveryModeUiAction
    data object SubmitNewPassword : RecoveryModeUiAction

    data object ExitClicked : RecoveryModeUiAction
    data object DismissPasswordDialog : RecoveryModeUiAction
}
