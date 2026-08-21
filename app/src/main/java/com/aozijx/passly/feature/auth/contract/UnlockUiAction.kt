package com.aozijx.passly.feature.auth.contract

import com.aozijx.passly.domain.access.model.AuthenticationMethod

sealed interface UnlockUiAction {
    data object BiometricClicked : UnlockUiAction
    data object LockIconClicked : UnlockUiAction
    data object BackPressed : UnlockUiAction
    data class AppPasswordChanged(val value: String) : UnlockUiAction
    data object AppPasswordSubmitted : UnlockUiAction
    data class RecoveryCodeChanged(val value: String) : UnlockUiAction
    data object RecoveryCodeSubmitted : UnlockUiAction
    data class InputExpanded(val method: AuthenticationMethod, val expanded: Boolean) :
        UnlockUiAction
    data object ClearVerificationFailure : UnlockUiAction
}
