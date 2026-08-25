package com.aozijx.passly.presentation.ui.settings.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordActionDialog
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordChangeDialog
import com.aozijx.passly.presentation.ui.settings.interaction.SwipeActionSelectDialog
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogState
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel

@Composable
internal fun SettingsScreenDialogsHost(
    state: SettingsDialogsModel,
    onEvent: (SettingsDialogEvent) -> Unit,
) {
    if (state.showRightActionDialog) {
        SwipeActionSelectDialog(
            stringResource(R.string.settings_swipe_select_right_action),
            state.swipeRightAction,
            {
                onEvent(SettingsDialogEvent.SetSwipeRightAction(it))
                onEvent(SettingsDialogEvent.DismissRightActionDialog)
            },
            { onEvent(SettingsDialogEvent.DismissRightActionDialog) }
        )
    }

    if (state.showLeftActionDialog) {
        SwipeActionSelectDialog(
            stringResource(R.string.settings_swipe_select_left_action),
            state.swipeLeftAction,
            {
                onEvent(SettingsDialogEvent.SetSwipeLeftAction(it))
                onEvent(SettingsDialogEvent.DismissLeftActionDialog)
            },
            { onEvent(SettingsDialogEvent.DismissLeftActionDialog) }
        )
    }

    if (state.showClearBackupDirConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                onEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
            },
            title = { Text(stringResource(R.string.settings_backup_clear_directory_title)) },
            text = { Text(stringResource(R.string.settings_backup_clear_directory_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(SettingsDialogEvent.ClearBackupDirectory)
                    onEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
                }) {
                    Text(stringResource(R.string.settings_backup_clear_selection))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    when (state.activeAppPasswordDialog) {
        AppPasswordDialogState.None -> Unit
        AppPasswordDialogState.Action -> {
            AppPasswordActionDialog(
                onDismiss = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissAction)
                    )
                },
                onChangePassword = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ShowChange)
                    )
                },
                onDisablePassword = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ShowDisable)
                    )
                }
            )
        }

        AppPasswordDialogState.Set -> {
            AppPasswordSetDialog(
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onNewPasswordChange = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.NewChanged(it))
                    )
                },
                onConfirmPasswordChange = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChanged(it))
                    )
                },
                onConfirm = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmSet)
                    )
                },
                onDismiss = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissSet)
                    )
                }
            )
        }

        AppPasswordDialogState.Change -> {
            AppPasswordChangeDialog(
                currentPassword = state.appPasswordCurrent,
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onCurrentPasswordChange = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.CurrentChanged(it))
                    )
                },
                onNewPasswordChange = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.NewChanged(it))
                    )
                },
                onConfirmPasswordChange = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChanged(it))
                    )
                },
                isConfirmEnabled = state.isChangePasswordConfirmEnabled,
                onConfirm = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChange)
                    )
                },
                onDismiss = {
                    onEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissChange)
                    )
                }
            )
        }
    }
}
