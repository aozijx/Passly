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

internal data class SettingsDialogsActions(
    val onSetSwipeRightAction: (SwipeActionType) -> Unit,
    val onSetSwipeLeftAction: (SwipeActionType) -> Unit,
    val onSetLockTimeout: (Long) -> Unit,
    val onClearBackupDirectory: () -> Unit,
    val onDismissRightActionDialog: () -> Unit,
    val onDismissLeftActionDialog: () -> Unit,
    val onDismissLockTimeoutDialog: () -> Unit,
    val onDismissClearBackupDirConfirmDialog: () -> Unit,
    val onDismissAppPasswordActionDialog: () -> Unit,
    val onShowChangeAppPasswordDialog: () -> Unit,
    val onShowDisableAppPasswordDialog: () -> Unit,
    val onDismissSetAppPasswordDialog: () -> Unit,
    val onDismissChangeAppPasswordDialog: () -> Unit,
    val onDismissDisableAppPasswordDialog: () -> Unit,
    val onAppPasswordCurrentChange: (String) -> Unit,
    val onAppPasswordNewChange: (String) -> Unit,
    val onAppPasswordConfirmChange: (String) -> Unit,
    val onConfirmSetAppPassword: () -> Unit,
    val onConfirmChangeAppPassword: () -> Unit,
    val onConfirmDisableAppPassword: () -> Unit
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
                onDismiss = actions.onDismissAppPasswordActionDialog,
                onChangePassword = {
                    actions.onDismissAppPasswordActionDialog()
                    actions.onShowChangeAppPasswordDialog()
                },
                onDisablePassword = {
                    actions.onDismissAppPasswordActionDialog()
                    actions.onShowDisableAppPasswordDialog()
                }
            )
        }

        AppPasswordDialog.Set -> {
            AppPasswordSetDialog(
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onNewPasswordChange = actions.onAppPasswordNewChange,
                onConfirmPasswordChange = actions.onAppPasswordConfirmChange,
                onConfirm = actions.onConfirmSetAppPassword,
                onDismiss = actions.onDismissSetAppPasswordDialog
            )
        }

        AppPasswordDialog.Change -> {
            AppPasswordChangeDialog(
                currentPassword = state.appPasswordCurrent,
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                onCurrentPasswordChange = actions.onAppPasswordCurrentChange,
                onNewPasswordChange = actions.onAppPasswordNewChange,
                onConfirmPasswordChange = actions.onAppPasswordConfirmChange,
                onConfirm = actions.onConfirmChangeAppPassword,
                onDismiss = actions.onDismissChangeAppPasswordDialog
            )
        }

        AppPasswordDialog.Disable -> {
            AppPasswordDisableDialog(
                currentPassword = state.appPasswordCurrent,
                onCurrentPasswordChange = actions.onAppPasswordCurrentChange,
                onConfirm = actions.onConfirmDisableAppPassword,
                onDismiss = actions.onDismissDisableAppPasswordDialog
            )
        }
    }
}