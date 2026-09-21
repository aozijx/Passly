package com.aozijx.passly.presentation.feature.settings.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.presentation.feature.settings.ui.main.model.AppPasswordDialogState

internal class AppPasswordDialogStateHolder {
    var activeAppPasswordDialog by mutableStateOf<AppPasswordDialogState>(AppPasswordDialogState.None)

    var appPasswordCurrent by mutableStateOf("")
    var appPasswordNew by mutableStateOf("")
    var appPasswordConfirm by mutableStateOf("")

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

    fun dismissSetAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
        clearAppPasswordInputs()
    }

    fun dismissChangeAppPasswordDialog() {
        activeAppPasswordDialog = AppPasswordDialogState.None
        clearAppPasswordInputs()
    }

    fun clearAppPasswordInputs() {
        appPasswordCurrent = ""
        appPasswordNew = ""
        appPasswordConfirm = ""
    }

    fun onAppPasswordSuccess() {
        activeAppPasswordDialog = AppPasswordDialogState.None
        clearAppPasswordInputs()
    }
}

@Composable
internal fun rememberAppPasswordDialogStateHolder(): AppPasswordDialogStateHolder {
    return remember { AppPasswordDialogStateHolder() }
}
