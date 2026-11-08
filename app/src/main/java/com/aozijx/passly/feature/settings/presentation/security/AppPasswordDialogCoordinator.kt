package com.aozijx.passly.feature.settings.presentation.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
import com.aozijx.passly.presentation.mapping.PasswordConfirmationUiState

internal class AppPasswordDialogCoordinator {
    private var activeDialog by mutableStateOf<AppPasswordDialog?>(null)
    private val currentPassword = SensitivePasswordDraft()
    private val newPassword = SensitivePasswordDraft()
    private val confirmPassword = SensitivePasswordDraft()

    val model: AppPasswordDialogsModel
        get() {
            val newPasswordSnapshot = newPassword.snapshot()
            return AppPasswordDialogsModel(
                activeAppPasswordDialog = activeDialog,
                appPasswordCurrent = currentPassword.snapshot(),
                appPasswordNew = newPasswordSnapshot,
                passwordStrength =
                    newPasswordSnapshot
                        .takeIf(String::isNotEmpty)
                        ?.let(PasswordStrengthEvaluator::evaluate),
                appPasswordConfirm = confirmPassword.snapshot(),
                confirmationState = confirmationState(),
                isSetPasswordConfirmEnabled =
                    passwordPolicyAcceptsNewPassword() &&
                        newPassword.contentEquals(confirmPassword),
                isChangePasswordConfirmEnabled =
                    currentPassword.isNotEmpty &&
                        passwordPolicyAcceptsNewPassword() &&
                        newPassword.contentEquals(confirmPassword),
            )
        }

    private fun confirmationState(): PasswordConfirmationUiState =
        if (confirmPassword.isNotEmpty && !newPassword.contentEquals(confirmPassword)) {
            PasswordConfirmationUiState.Mismatch
        } else {
            PasswordConfirmationUiState.Hidden
        }

    fun onAppPasswordEntryAuthorized(alreadyEnabled: Boolean) {
        clearDrafts()
        activeDialog =
            if (alreadyEnabled) {
                AppPasswordDialog.ACTION
            } else {
                AppPasswordDialog.SET
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
                if (activeDialog == AppPasswordDialog.ACTION) closeAndClear()
                null
            }

            AppPasswordDialogEvent.ShowChange -> {
                if (activeDialog == AppPasswordDialog.ACTION) {
                    activeDialog = AppPasswordDialog.CHANGE
                }
                null
            }

            AppPasswordDialogEvent.ShowDisable -> {
                AppPasswordChangeRequest.Disable.takeIf { activeDialog == AppPasswordDialog.ACTION }
            }

            AppPasswordDialogEvent.DismissSet,
            AppPasswordDialogEvent.DismissChange,
            -> {
                val isActiveDismissal =
                    (event == AppPasswordDialogEvent.DismissSet && activeDialog == AppPasswordDialog.SET) ||
                        (event == AppPasswordDialogEvent.DismissChange && activeDialog == AppPasswordDialog.CHANGE)
                if (isActiveDismissal) closeAndClear()
                null
            }

            is AppPasswordDialogEvent.CurrentChanged -> {
                if (activeDialog == AppPasswordDialog.CHANGE) {
                    currentPassword.replace(event.value.toCharArray())
                }
                null
            }

            is AppPasswordDialogEvent.NewChanged -> {
                if (activeDialog?.acceptsPasswordDraftChanges == true) {
                    newPassword.replace(event.value.toCharArray())
                }
                null
            }

            is AppPasswordDialogEvent.ConfirmChanged -> {
                if (activeDialog?.acceptsPasswordDraftChanges == true) {
                    confirmPassword.replace(event.value.toCharArray())
                }
                null
            }

            AppPasswordDialogEvent.ConfirmSet -> {
                if (activeDialog == AppPasswordDialog.SET) {
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
                if (activeDialog == AppPasswordDialog.CHANGE) {
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
        activeDialog = null
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
