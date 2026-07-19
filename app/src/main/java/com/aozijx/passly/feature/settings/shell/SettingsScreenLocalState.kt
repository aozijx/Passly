package com.aozijx.passly.feature.settings.shell

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.feature.settings.apppassword.AppPasswordAction
import com.aozijx.passly.feature.settings.internal.AppPasswordDialogState

@OptIn(ExperimentalMaterial3Api::class)
internal class SettingsScreenLocalState {
    var showLeftActionDialog by mutableStateOf(false)
    var showRightActionDialog by mutableStateOf(false)
    var showClearBackupDirConfirmDialog by mutableStateOf(false)
    var activeAppPasswordDialog by mutableStateOf<AppPasswordDialogState>(AppPasswordDialogState.None)
    var showRecoveryCodeSheet by mutableStateOf(false)
    lateinit var recoveryCodeSheetState: SheetState

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

    fun openClearBackupDirConfirmDialog() {
        showClearBackupDirConfirmDialog = true
    }

    fun dismissClearBackupDirConfirmDialog() {
        showClearBackupDirConfirmDialog = false
    }

    fun openAppPasswordActionDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.Action
    }

    fun dismissAppPasswordActionDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
    }

    fun openSetAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.Set
    }

    fun openChangeAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.Change
    }

    fun openDisableAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.Disable
    }

    fun dismissSetAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
        clearAppPasswordInputs()
    }

    fun dismissChangeAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
        clearAppPasswordInputs()
    }

    fun dismissDisableAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
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
            AppPasswordAction.DISABLE -> activeAppPasswordDialog = AppPasswordDialogState.None
        }
        clearAppPasswordInputs()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSettingsScreenLocalState(): SettingsScreenLocalState {
    val state = remember { SettingsScreenLocalState() }
    state.recoveryCodeSheetState = rememberModalBottomSheetState()
    return state
}