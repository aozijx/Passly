package com.aozijx.passly.ui.features.settings.shell

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.aozijx.passly.R
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordActionDialog
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordChangeDialog
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordDisableDialog
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordSetDialog
import com.aozijx.passly.ui.features.settings.interaction.SwipeActionSelectDialog
import com.aozijx.passly.ui.features.settings.internal.AppPasswordDialogEvent
import com.aozijx.passly.ui.features.settings.internal.AppPasswordDialogState
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogEvent
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsActions
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsState

@Composable
internal fun SettingsScreenDialogsHost(
    state: SettingsDialogsState,
    actions: SettingsDialogsActions
) {
    if (state.showRightActionDialog) {
        SwipeActionSelectDialog(
            "选择右滑动作",
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
            "选择左滑动作",
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
            title = { Text("清除备份目录") },
            text = { Text("只会清除目录配置，不会删除已导出的备份文件。") },
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
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    actions.onDialogEvent(SettingsDialogEvent.DismissClearBackupDirConfirmDialog)
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.showDeviceCredentialFallbackWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                actions.onDialogEvent(SettingsDialogEvent.DismissDeviceCredentialFallbackWarningDialog)
            },
            title = { Text(state.context.getString(R.string.settings_device_credential_warning_title)) },
            text = { Text(state.context.getString(R.string.settings_device_credential_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDialogEvent(SettingsDialogEvent.ConfirmEnableDeviceCredentialFallback)
                }) {
                    Text(state.context.getString(R.string.settings_device_credential_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    actions.onDialogEvent(SettingsDialogEvent.DismissDeviceCredentialFallbackWarningDialog)
                }) {
                    Text(state.context.getString(R.string.action_cancel))
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

        AppPasswordDialogState.Disable -> {
            AppPasswordDisableDialog(
                currentPassword = state.appPasswordCurrent,
                onCurrentPasswordChange = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.CurrentChanged(it))
                    )
                },
                onConfirm = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.ConfirmDisable)
                    )
                },
                onDismiss = {
                    actions.onDialogEvent(
                        SettingsDialogEvent.AppPassword(AppPasswordDialogEvent.DismissDisable)
                    )
                }
            )
        }
    }
}