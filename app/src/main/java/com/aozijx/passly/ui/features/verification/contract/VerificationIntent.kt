package com.aozijx.passly.ui.features.verification.contract

import androidx.fragment.app.FragmentActivity

sealed interface VerificationIntent {
    data class VerifyWithBiometric(
        val activity: FragmentActivity,
        val title: String,
        val subtitle: String
    ) : VerificationIntent

    data object VerifyWithAppPassword : VerificationIntent
    data class PasswordChange(val value: String) : VerificationIntent
    data class PasswordConfirmChange(val value: String) : VerificationIntent
    data object ShowPasswordInput : VerificationIntent
    data object ShowSetPasswordDialog : VerificationIntent
    data object DismissSetPasswordDialog : VerificationIntent
    data class BootstrapAppPassword(val onComplete: (Boolean) -> Unit) : VerificationIntent
}