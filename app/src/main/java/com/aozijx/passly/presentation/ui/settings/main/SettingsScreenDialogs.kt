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
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsActions
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel

@Composable
internal fun SettingsScreenDialogsHost(
    state: SettingsDialogsModel,
    actions: SettingsDialogsActions
) {
    if (state.showRightActionDialog) {
        SwipeActionSelectDialog(
            stringResource(R.string.settings_swipe_select_right_action),
            state.swipeRightAction,
            {
                actions.onDialogEvent(SettingsDialogEvent.SetSwipeRightAction(it))
                actions.onDialogEvent(SettingsDialogEvent.DismissRightActionDialog)
            },
            { actions.onDialogEvent(SettingsDialogEvent.DismissRightActionDialog) }
        )
    }

    if (state.showLeftActionDialog) {
        SwipeActionSelectDialog(
            stringResource(R.string.settings_swipe_select_left_action),
            state.swipeLeftAction,
            {
                actions.onDialogEvent(SettingsDialogEvent.SetSwipeLeftAction(it))
                actions.onDialogEvent(SettingsDialogEvent.DismissLeftActionDialog)
            },
            { actions.onDialogEvent(SettingsDialogEvent.DismissLeftActionDialog) }
        )
    }

    if (state.showClearBackupDirConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                actions.onDialogEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
            },
            title = { Text(stringResource(R.string.settings_backup_clear_directory_title)) },
            text = { Text(stringResource(R.string.settings_backup_clear_directory_message)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDialogEvent(SettingsDialogEvent.ClearBackupDirectory)
                    actions.onDialogEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
                }) {
                    Text(stringResource(R.string.settings_backup_clear_selection))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    actions.onDialogEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
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
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissAction)
                    )
                },
                onChangePassword = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ShowChange)
                    )
                },
                onDisablePassword = {
                    actions.onDialogEvent(
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
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.NewChanged(it))
                    )
                },
                onConfirmPasswordChange = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChanged(it))
                    )
                },
                onConfirm = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmSet)
                    )
                },
                onDismiss = {
                    actions.onDialogEvent(
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
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.CurrentChanged(it))
                    )
                },
                onNewPasswordChange = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.NewChanged(it))
                    )
                },
                onConfirmPasswordChange = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChanged(it))
                    )
                },
                isConfirmEnabled = state.isChangePasswordConfirmEnabled,
                onConfirm = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmChange)
                    )
                },
                onDismiss = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissChange)
                    )
                }
            )
        }
    }
}
