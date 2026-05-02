package com.aozijx.passly.features.settings

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordActionDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordChangeDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordDisableDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordSetDialog
import com.aozijx.passly.features.settings.components.dialogs.LockTimeoutDialog
import com.aozijx.passly.features.settings.components.dialogs.SwipeActionSelectDialog
import com.aozijx.passly.features.settings.internal.AppPasswordDialog

internal data class SettingsDialogsState(
    val showRightActionDialog: Boolean,
    val showLeftActionDialog: Boolean,
    val showLockTimeoutDialog: Boolean,
    val showClearBackupDirConfirmDialog: Boolean,
    val activeAppPasswordDialog: AppPasswordDialog,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val lockTimeout: Long,
    val backupDirectoryUri: String?,
    val context: Context,
    val appPasswordCurrent: String,
    val appPasswordNew: String,
    val appPasswordConfirm: String
)

internal sealed interface AppPasswordDialogEvent {
    data object DismissAction : AppPasswordDialogEvent
    data object ShowChange : AppPasswordDialogEvent
    data object ShowDisable : AppPasswordDialogEvent
    data object DismissSet : AppPasswordDialogEvent
    data object DismissChange : AppPasswordDialogEvent
    data object DismissDisable : AppPasswordDialogEvent
    data class CurrentChanged(val value: String) : AppPasswordDialogEvent
    data class NewChanged(val value: String) : AppPasswordDialogEvent
    data class ConfirmChanged(val value: String) : AppPasswordDialogEvent
    data object ConfirmSet : AppPasswordDialogEvent
    data object ConfirmChange : AppPasswordDialogEvent
    data object ConfirmDisable : AppPasswordDialogEvent
}

internal sealed interface SettingsDialogEvent {
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsDialogEvent
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsDialogEvent
    data class SetLockTimeout(val timeoutMs: Long) : SettingsDialogEvent
    data object ClearBackupDirectory : SettingsDialogEvent
    data object DismissRightActionDialog : SettingsDialogEvent
    data object DismissLeftActionDialog : SettingsDialogEvent
    data object DismissLockTimeoutDialog : SettingsDialogEvent
    data object DismissClearBackupDirConfirmDialog : SettingsDialogEvent
    data class AppPassword(val event: AppPasswordDialogEvent) : SettingsDialogEvent
}

internal data class SettingsDialogsActions(
    val onDialogEvent: (SettingsDialogEvent) -> Unit
)

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

    if (state.showLockTimeoutDialog) {
        LockTimeoutDialog(
            currentTimeoutMs = state.lockTimeout,
            onTimeoutSelected = {
                actions.onDialogEvent(SettingsDialogEvent.SetLockTimeout(it))
                actions.onDialogEvent(SettingsDialogEvent.DismissLockTimeoutDialog)
            },
            onDismiss = { actions.onDialogEvent(SettingsDialogEvent.DismissLockTimeoutDialog) }
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
        AppPasswordDialog.None -> Unit
        AppPasswordDialog.Action -> {
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

        AppPasswordDialog.Set -> {
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

        AppPasswordDialog.Change -> {
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

        AppPasswordDialog.Disable -> {
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