package com.aozijx.passly.presentation.feature.settings.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest

internal class AppPasswordDialogCoordinator {
    private var activeDialog by mutableStateOf<AppPasswordDialogState>(AppPasswordDialogState.None)
    private var currentPassword by mutableStateOf("")
    private var newPassword by mutableStateOf("")
    private var confirmPassword by mutableStateOf("")

    val model: AppPasswordDialogsModel
        get() = AppPasswordDialogsModel(
            activeAppPasswordDialog = activeDialog,
            appPasswordCurrent = currentPassword,
            appPasswordNew = newPassword,
            appPasswordConfirm = confirmPassword,
            isSetPasswordConfirmEnabled = passwordPolicyAcceptsNewPassword() &&
                newPassword == confirmPassword,
            isChangePasswordConfirmEnabled = currentPassword.isNotEmpty() &&
                passwordPolicyAcceptsNewPassword() &&
                newPassword == confirmPassword,
        )

    fun onAppPasswordEntryAuthorized(alreadyEnabled: Boolean) {
        activeDialog = if (alreadyEnabled) {
            AppPasswordDialogState.Action
        } else {
            AppPasswordDialogState.Set
        }
    }

    fun onAppPasswordOperationSucceeded() {
        closeAndClear()
    }

    fun onEvent(event: AppPasswordDialogEvent): AppPasswordChangeRequest? = when (event) {
        AppPasswordDialogEvent.DismissAction -> {
            activeDialog = AppPasswordDialogState.None
            null
        }
        AppPasswordDialogEvent.ShowChange -> {
            activeDialog = AppPasswordDialogState.Change
            null
        }
        AppPasswordDialogEvent.ShowDisable -> AppPasswordChangeRequest.Disable
        AppPasswordDialogEvent.DismissSet,
        AppPasswordDialogEvent.DismissChange -> {
            closeAndClear()
            null
        }
        is AppPasswordDialogEvent.CurrentChanged -> {
            currentPassword = event.value
            null
        }
        is AppPasswordDialogEvent.NewChanged -> {
            newPassword = event.value
            null
        }
        is AppPasswordDialogEvent.ConfirmChanged -> {
            confirmPassword = event.value
            null
        }
        AppPasswordDialogEvent.ConfirmSet -> AppPasswordChangeRequest.Set(
            password = newPassword.toCharArray(),
            confirmation = confirmPassword.toCharArray(),
        )
        AppPasswordDialogEvent.ConfirmChange -> AppPasswordChangeRequest.Change(
            currentPassword = currentPassword.toCharArray(),
            newPassword = newPassword.toCharArray(),
            confirmation = confirmPassword.toCharArray(),
        )
    }

    private fun passwordPolicyAcceptsNewPassword(): Boolean =
        AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length)

    private fun closeAndClear() {
        activeDialog = AppPasswordDialogState.None
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
    }
}

@Composable
internal fun rememberAppPasswordDialogCoordinator(): AppPasswordDialogCoordinator =
    remember { AppPasswordDialogCoordinator() }
