package com.aozijx.passly.features.settings.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal class SettingsScreenLocalState {
    var showLeftActionDialog by mutableStateOf(false)
    var showRightActionDialog by mutableStateOf(false)
    var showLockTimeoutDialog by mutableStateOf(false)
    var showClearBackupDirConfirmDialog by mutableStateOf(false)
    var showAppPasswordActionDialog by mutableStateOf(false)
    var showSetAppPasswordDialog by mutableStateOf(false)
    var showChangeAppPasswordDialog by mutableStateOf(false)
    var showDisableAppPasswordDialog by mutableStateOf(false)

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

    fun openAppPasswordActionDialog() {
        dismissAppPasswordDialogs(clearInputs = false)
        showAppPasswordActionDialog = true
    }

    fun dismissAppPasswordActionDialog() {
        showAppPasswordActionDialog = false
    }

    fun openSetAppPasswordDialog() {
        dismissAppPasswordDialogs(clearInputs = false)
        showSetAppPasswordDialog = true
    }

    fun openChangeAppPasswordDialog() {
        dismissAppPasswordDialogs(clearInputs = false)
        showChangeAppPasswordDialog = true
    }

    fun openDisableAppPasswordDialog() {
        dismissAppPasswordDialogs(clearInputs = false)
        showDisableAppPasswordDialog = true
    }

    fun dismissSetAppPasswordDialog() {
        showSetAppPasswordDialog = false
        clearAppPasswordInputs()
    }

    fun dismissChangeAppPasswordDialog() {
        showChangeAppPasswordDialog = false
        clearAppPasswordInputs()
    }

    fun dismissDisableAppPasswordDialog() {
        showDisableAppPasswordDialog = false
        clearAppPasswordInputs()
    }

    fun clearAppPasswordInputs() {
        appPasswordCurrent = ""
        appPasswordNew = ""
        appPasswordConfirm = ""
    }

    fun onAppPasswordSuccess(action: AppPasswordAction) {
        when (action) {
            AppPasswordAction.SET -> showSetAppPasswordDialog = false
            AppPasswordAction.CHANGE -> showChangeAppPasswordDialog = false
            AppPasswordAction.DISABLE -> showDisableAppPasswordDialog = false
        }
        clearAppPasswordInputs()
    }

    private fun dismissAppPasswordDialogs(clearInputs: Boolean) {
        showAppPasswordActionDialog = false
        showSetAppPasswordDialog = false
        showChangeAppPasswordDialog = false
        showDisableAppPasswordDialog = false
        if (clearInputs) clearAppPasswordInputs()
    }
}

@Composable
internal fun rememberSettingsScreenLocalState(): SettingsScreenLocalState =
    remember { SettingsScreenLocalState() }