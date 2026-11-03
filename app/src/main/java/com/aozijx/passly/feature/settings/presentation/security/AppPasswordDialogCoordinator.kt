package com.aozijx.passly.feature.settings.presentation.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest

internal class AppPasswordDialogCoordinator {
    private var activeDialog by mutableStateOf<AppPasswordDialogState>(AppPasswordDialogState.None)
    private val currentPassword = SensitivePasswordDraft()
    private val newPassword = SensitivePasswordDraft()
    private val confirmPassword = SensitivePasswordDraft()

    val model: AppPasswordDialogsModel
        get() =
            AppPasswordDialogsModel(
                activeAppPasswordDialog = activeDialog,
                appPasswordCurrent = currentPassword.snapshot(),
                appPasswordNew = newPassword.snapshot(),
                appPasswordConfirm = confirmPassword.snapshot(),
                isSetPasswordConfirmEnabled =
                    passwordPolicyAcceptsNewPassword() &&
                        newPassword.contentEquals(confirmPassword),
                isChangePasswordConfirmEnabled =
                    currentPassword.isNotEmpty &&
                        passwordPolicyAcceptsNewPassword() &&
                        newPassword.contentEquals(confirmPassword),
            )

    fun onAppPasswordEntryAuthorized(alreadyEnabled: Boolean) {
        clearDrafts()
        activeDialog =
            if (alreadyEnabled) {
                AppPasswordDialogState.Action
            } else {
                AppPasswordDialogState.Set
            }
    }

    fun onAppPasswordOperationSucceeded() {
        closeAndClear()
    }

    fun onEvent(
        event: AppPasswordDialogEvent,
        isBusy: Boolean = false,
    ): AppPasswordChangeRequest? {
        if (isBusy) return null
        return when (event) {
            AppPasswordDialogEvent.DismissAction -> {
                if (activeDialog == AppPasswordDialogState.Action) closeAndClear()
                null
            }

            AppPasswordDialogEvent.ShowChange -> {
                if (activeDialog == AppPasswordDialogState.Action) {
                    activeDialog = AppPasswordDialogState.Change
                }
                null
            }

            AppPasswordDialogEvent.ShowDisable -> {
                AppPasswordChangeRequest.Disable.takeIf { activeDialog == AppPasswordDialogState.Action }
            }

            AppPasswordDialogEvent.DismissSet,
            AppPasswordDialogEvent.DismissChange,
            -> {
                val isActiveDismissal =
                    (event == AppPasswordDialogEvent.DismissSet && activeDialog == AppPasswordDialogState.Set) ||
                        (event == AppPasswordDialogEvent.DismissChange && activeDialog == AppPasswordDialogState.Change)
                if (isActiveDismissal) closeAndClear()
                null
            }

            is AppPasswordDialogEvent.CurrentChanged -> {
                if (activeDialog == AppPasswordDialogState.Change) {
                    currentPassword.replace(event.value.toCharArray())
                }
                null
            }

            is AppPasswordDialogEvent.NewChanged -> {
                if (activeDialog == AppPasswordDialogState.Set || activeDialog == AppPasswordDialogState.Change) {
                    newPassword.replace(event.value.toCharArray())
                }
                null
            }

            is AppPasswordDialogEvent.ConfirmChanged -> {
                if (activeDialog == AppPasswordDialogState.Set || activeDialog == AppPasswordDialogState.Change) {
                    confirmPassword.replace(event.value.toCharArray())
                }
                null
            }

            AppPasswordDialogEvent.ConfirmSet -> {
                if (activeDialog == AppPasswordDialogState.Set) {
                    val request =
                        AppPasswordChangeRequest.Set(
                            password = newPassword.copy(),
                            confirmation = confirmPassword.copy(),
                        )
                    clearDrafts()
                    request
                } else {
                    null
                }
            }

            AppPasswordDialogEvent.ConfirmChange -> {
                if (activeDialog == AppPasswordDialogState.Change) {
                    val request =
                        AppPasswordChangeRequest.Change(
                            currentPassword = currentPassword.copy(),
                            newPassword = newPassword.copy(),
                            confirmation = confirmPassword.copy(),
                        )
                    clearDrafts()
                    request
                } else {
                    null
                }
            }
        }
    }

    fun clear() {
        closeAndClear()
    }

    private fun passwordPolicyAcceptsNewPassword(): Boolean = AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.size)

    private fun closeAndClear() {
        activeDialog = AppPasswordDialogState.None
        clearDrafts()
    }

    private fun clearDrafts() {
        currentPassword.clear()
        newPassword.clear()
        confirmPassword.clear()
    }
}

@Composable
internal fun rememberAppPasswordDialogCoordinator(): AppPasswordDialogCoordinator = remember { AppPasswordDialogCoordinator() }

@Composable
internal fun AppPasswordDialogCoordinatorOwner(coordinator: AppPasswordDialogCoordinator) {
    DisposableEffect(coordinator) {
        onDispose(coordinator::clear)
    }
}
