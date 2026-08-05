package com.aozijx.passly.feature.auth.presentation

import com.aozijx.passly.domain.authentication.AuthenticationMethod

sealed interface AuthenticationIntent {
    data object BiometricClicked : AuthenticationIntent
    data class AppPasswordChanged(val value: String) : AuthenticationIntent
    data object AppPasswordSubmitted : AuthenticationIntent
    data class RecoveryCodeChanged(val value: String) : AuthenticationIntent
    data object RecoveryCodeSubmitted : AuthenticationIntent
    data class InputExpanded(val method: AuthenticationMethod, val expanded: Boolean) :
        AuthenticationIntent

    data class NewAppPasswordChanged(val value: String) : AuthenticationIntent
    data class ConfirmAppPasswordChanged(val value: String) : AuthenticationIntent
    data object SetPasswordClicked : AuthenticationIntent
    data object SetPasswordConfirmed : AuthenticationIntent
    data object DismissSetPasswordDialog : AuthenticationIntent
    data object ClearVerificationFailure : AuthenticationIntent
}