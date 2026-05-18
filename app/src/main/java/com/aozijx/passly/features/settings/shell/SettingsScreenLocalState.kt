package com.aozijx.passly.features.settings.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.features.backup.ui.BackupPathSettingsConfig
import com.aozijx.passly.features.settings.apppassword.AppPasswordAction

internal sealed interface AppPasswordDialog {
    data object None : AppPasswordDialog
    data object Action : AppPasswordDialog
    data object Set : AppPasswordDialog
    data object Change : AppPasswordDialog
    data object Disable : AppPasswordDialog
}

internal class SettingsScreenLocalState {
    var showLeftActionDialog by mutableStateOf(false)
    var showRightActionDialog by mutableStateOf(false)
    var showLockTimeoutDialog by mutableStateOf(false)
    var showClearBackupDirConfirmDialog by mutableStateOf(false)
    var showDeviceCredentialFallbackWarningDialog by mutableStateOf(false)
    var activeAppPasswordDialog by mutableStateOf<AppPasswordDialog>(AppPasswordDialog.None)

    var appPasswordCurrent by mutableStateOf("")
    var appPasswordNew by mutableStateOf("")
    var appPasswordConfirm by mutableStateOf("")

    fun openLeftActionDialog() {
        showRightActionDialog = false
        showLeftActionDialog = true
    }

    fun openRightActionDialog() {
        showLeftActionDialog = false
        showRightActionDialog = true
    }

    fun dismissLeftActionDialog() {
        showLeftActionDialog = false
    }

    fun dismissRightActionDialog() {
        showRightActionDialog = false
    }

    fun openLockTimeoutDialog() {
        showLockTimeoutDialog = true
    }

    fun dismissLockTimeoutDialog() {
        showLockTimeoutDialog = false
    }

    fun openClearBackupDirConfirmDialog() {
        showClearBackupDirConfirmDialog = true
    }

    fun dismissClearBackupDirConfirmDialog() {
        showClearBackupDirConfirmDialog = false
    }

    fun openDeviceCredentialFallbackWarningDialog() {
        showDeviceCredentialFallbackWarningDialog = true
    }

    fun dismissDeviceCredentialFallbackWarningDialog() {
        showDeviceCredentialFallbackWarningDialog = false
    }

    fun openAppPasswordActionDialog() {
        setActiveAppPasswordDialog(AppPasswordDialog.Action, clearInputs = false)
    }

    fun dismissAppPasswordActionDialog() {
        activeAppPasswordDialog = AppPasswordDialog.None
    }

    fun openSetAppPasswordDialog() {
        setActiveAppPasswordDialog(AppPasswordDialog.Set, clearInputs = false)
    }

    fun openChangeAppPasswordDialog() {
        setActiveAppPasswordDialog(AppPasswordDialog.Change, clearInputs = false)
    }

    fun openDisableAppPasswordDialog() {
        setActiveAppPasswordDialog(AppPasswordDialog.Disable, clearInputs = false)
    }

    fun dismissSetAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialog.None
        clearAppPasswordInputs()
    }

    fun dismissChangeAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialog.None
        clearAppPasswordInputs()
    }

    fun dismissDisableAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialog.None
        clearAppPasswordInputs()
    }

    fun clearAppPasswordInputs() {
        appPasswordCurrent = ""
        appPasswordNew = ""
        appPasswordConfirm = ""
    }

    fun onAppPasswordSuccess(action: AppPasswordAction) {
        when (action) {
            AppPasswordAction.SET,
            AppPasswordAction.CHANGE,
            AppPasswordAction.DISABLE -> activeAppPasswordDialog = AppPasswordDialog.None
        }
        clearAppPasswordInputs()
    }

    fun backupPathLabel(rawUri: String?): String = BackupPathSettingsConfig.displayValue(rawUri)

    fun lastExportFileLabel(fileName: String?): String =
        BackupPathSettingsConfig.displayRecentFileName(fileName)

    private fun setActiveAppPasswordDialog(dialog: AppPasswordDialog, clearInputs: Boolean) {
        activeAppPasswordDialog = dialog
        if (clearInputs) clearAppPasswordInputs()
    }
}

@Composable
internal fun rememberSettingsScreenLocalState(): SettingsScreenLocalState =
    remember { SettingsScreenLocalState() }