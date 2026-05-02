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

internal data class SettingsDialogsActions(
    val onSetSwipeRightAction: (SwipeActionType) -> Unit,
    val onSetSwipeLeftAction: (SwipeActionType) -> Unit,
    val onSetLockTimeout: (Long) -> Unit,
    val onClearBackupDirectory: () -> Unit,
    val onDismissRightActionDialog: () -> Unit,
    val onDismissLeftActionDialog: () -> Unit,
    val onDismissLockTimeoutDialog: () -> Unit,
    val onDismissClearBackupDirConfirmDialog: () -> Unit,
    val onAppPasswordEvent: (AppPasswordDialogEvent) -> Unit
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
                actions.onSetSwipeRightAction(it)
                actions.onDismissRightActionDialog()
            },
            actions.onDismissRightActionDialog
        )
    }

    if (state.showLeftActionDialog) {
        SwipeActionSelectDialog(
            "选择左滑动作",
            state.swipeLeftAction,
            {
                actions.onSetSwipeLeftAction(it)
                actions.onDismissLeftActionDialog()
            },
            actions.onDismissLeftActionDialog
        )
    }

    if (state.showLockTimeoutDialog) {
        LockTimeoutDialog(
            currentTimeoutMs = state.lockTimeout,
            onTimeoutSelected = {
                actions.onSetLockTimeout(it)
                actions.onDismissLockTimeoutDialog()
            },
            onDismiss = actions.onDismissLockTimeoutDialog
        )
    }

    if (state.showClearBackupDirConfirmDialog) {
        AlertDialog(
            onDismissRequest = actions.onDismissClearBackupDirConfirmDialog,
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
                    actions.onClearBackupDirectory()
                    actions.onDismissClearBackupDirConfirmDialog()
                }) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissClearBackupDirConfirmDialog) {
                    Text("取消")
                }
            }
        )
    }

    when (state.activeAppPasswordDialog) {
        AppPasswordDialog.None -> Unit
        AppPasswordDialog.Action -> {
            AppPasswordActionDialog(
                onDismiss = { actions.onAppPasswordEvent(AppPasswordDialogEvent.DismissAction) },
                onChangePassword = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.ShowChange)
                },
                onDisablePassword = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.ShowDisable)
                }
            )
        }

        AppPasswordDialog.Set -> {
            AppPasswordSetDialog(
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onNewPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.NewChanged(it))
                },
                onConfirmPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.ConfirmChanged(it))
                },
                onConfirm = { actions.onAppPasswordEvent(AppPasswordDialogEvent.ConfirmSet) },
                onDismiss = { actions.onAppPasswordEvent(AppPasswordDialogEvent.DismissSet) }
            )
        }

        AppPasswordDialog.Change -> {
            AppPasswordChangeDialog(
                currentPassword = state.appPasswordCurrent,
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onCurrentPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.CurrentChanged(it))
                },
                onNewPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.NewChanged(it))
                },
                onConfirmPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.ConfirmChanged(it))
                },
                onConfirm = { actions.onAppPasswordEvent(AppPasswordDialogEvent.ConfirmChange) },
                onDismiss = { actions.onAppPasswordEvent(AppPasswordDialogEvent.DismissChange) }
            )
        }

        AppPasswordDialog.Disable -> {
            AppPasswordDisableDialog(
                currentPassword = state.appPasswordCurrent,
                onCurrentPasswordChange = {
                    actions.onAppPasswordEvent(AppPasswordDialogEvent.CurrentChanged(it))
                },
                onConfirm = { actions.onAppPasswordEvent(AppPasswordDialogEvent.ConfirmDisable) },
                onDismiss = { actions.onAppPasswordEvent(AppPasswordDialogEvent.DismissDisable) }
            )
        }
    }
}