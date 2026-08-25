package com.aozijx.passly.presentation.feature.settings.main

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordActionDialog
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordChangeDialog
import com.aozijx.passly.presentation.feature.settings.main.interaction.SwipeActionSelectDialog
import com.aozijx.passly.presentation.feature.settings.main.component.AppPasswordDialogEvent
import com.aozijx.passly.presentation.feature.settings.main.component.AppPasswordDialogState
import com.aozijx.passly.presentation.feature.settings.main.component.SettingsDialogEvent
import com.aozijx.passly.presentation.feature.settings.main.component.SettingsDialogsActions
import com.aozijx.passly.presentation.feature.settings.main.component.SettingsDialogsState

@Composable
internal fun SettingsScreenDialogsHost(
    state: SettingsDialogsState,
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
                    if (!state.backupDirectoryUri.isNullOrBlank()) {
                        runCatching<Unit> {
                            val uri = state.backupDirectoryUri.toUri()
                            val flags =
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            state.context.contentResolver.releasePersistableUriPermission(
                                uri,
                                flags
                            )
                        }
                    }
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
                isConfirmEnabled = state.appPasswordCurrent.isNotEmpty() &&
                        AppPasswordPolicy.DEFAULT.acceptsLength(state.appPasswordNew.length) &&
                        state.appPasswordNew == state.appPasswordConfirm,
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
