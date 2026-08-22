package com.aozijx.passly.feature.auth.contract

sealed interface BootstrapUiAction {
    data class NewAppPasswordChanged(val value: String) : BootstrapUiAction
    data class ConfirmAppPasswordChanged(val value: String) : BootstrapUiAction
    data object SetPasswordClicked : BootstrapUiAction
    data object SetPasswordConfirmed : BootstrapUiAction
    data object DismissSetPasswordDialog : BootstrapUiAction
}
