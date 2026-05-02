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
}

@Composable
internal fun rememberSettingsScreenLocalState(): SettingsScreenLocalState =
    remember { SettingsScreenLocalState() }