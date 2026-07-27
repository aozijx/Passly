package com.aozijx.passly.feature.settings.shell

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordActionDialog
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordChangeDialog
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordSetDialog
import com.aozijx.passly.feature.settings.interaction.SwipeActionSelectDialog
import com.aozijx.passly.feature.settings.internal.AppPasswordDialogEvent
import com.aozijx.passly.feature.settings.internal.AppPasswordDialogState
import com.aozijx.passly.feature.settings.internal.SettingsDialogEvent
import com.aozijx.passly.feature.settings.internal.SettingsDialogsActions
import com.aozijx.passly.feature.settings.internal.SettingsDialogsState

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
    }
}
