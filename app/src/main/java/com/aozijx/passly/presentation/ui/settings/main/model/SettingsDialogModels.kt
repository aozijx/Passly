package com.aozijx.passly.presentation.feature.settings.main.component

import android.content.Context
import com.aozijx.passly.domain.settings.model.SwipeActionType

internal sealed interface AppPasswordDialogState {
    data object None : AppPasswordDialogState
    data object Action : AppPasswordDialogState
    data object Set : AppPasswordDialogState
    data object Change : AppPasswordDialogState
}

internal data class SettingsDialogsState(
    val showRightActionDialog: Boolean,
    val showLeftActionDialog: Boolean,
    val showClearBackupDirConfirmDialog: Boolean,
    val activeAppPasswordDialog: AppPasswordDialogState,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val backupDirectoryUri: String?,
    val context: Context,
    val appPasswordCurrent: String,
    val appPasswordNew: String,
    val appPasswordConfirm: String
)

internal sealed interface AppPasswordDialogEvent {
    data object DismissAction : AppPasswordDialogEvent
    data object ShowChange : AppPasswordDialogEvent
    data object DismissSet : AppPasswordDialogEvent
    data object DismissChange : AppPasswordDialogEvent
    data class CurrentChanged(val value: String) : AppPasswordDialogEvent
    data class NewChanged(val value: String) : AppPasswordDialogEvent
    data class ConfirmChanged(val value: String) : AppPasswordDialogEvent
    data object ConfirmSet : AppPasswordDialogEvent
    data object ConfirmChange : AppPasswordDialogEvent
    data object ShowDisable : AppPasswordDialogEvent
}

internal sealed interface SettingsDialogEvent {
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsDialogEvent
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsDialogEvent
    data object ClearBackupDirectory : SettingsDialogEvent
    data object DismissRightActionDialog : SettingsDialogEvent
    data object DismissLeftActionDialog : SettingsDialogEvent
    data object DismissClearBackupDirConfirmDialog : SettingsDialogEvent
    data class AppPassword(val event: AppPasswordDialogEvent) : SettingsDialogEvent
}

internal data class SettingsDialogsActions(
    val onDialogEvent: (SettingsDialogEvent) -> Unit
)
