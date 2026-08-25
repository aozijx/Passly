package com.aozijx.passly.presentation.ui.settings.main.model

import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel

internal sealed interface AppPasswordDialogState {
    data object None : AppPasswordDialogState
    data object Action : AppPasswordDialogState
    data object Set : AppPasswordDialogState
    data object Change : AppPasswordDialogState
}
internal data class SettingsDialogsModel(
    val showRightActionDialog: Boolean,
    val showLeftActionDialog: Boolean,
    val showClearBackupDirConfirmDialog: Boolean,
    val activeAppPasswordDialog: AppPasswordDialogState,
    val swipeLeftAction: VaultSwipeActionUiModel,
    val swipeRightAction: VaultSwipeActionUiModel,
    val appPasswordCurrent: String,
    val appPasswordNew: String,
    val appPasswordConfirm: String,
    val isChangePasswordConfirmEnabled: Boolean,
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
    data class SetSwipeRightAction(val action: VaultSwipeActionUiModel) : SettingsDialogEvent
    data class SetSwipeLeftAction(val action: VaultSwipeActionUiModel) : SettingsDialogEvent
    data object ClearBackupDirectory : SettingsDialogEvent
    data object DismissRightActionDialog : SettingsDialogEvent
    data object DismissLeftActionDialog : SettingsDialogEvent
    data object DismissClearBackupDirConfirmDialog : SettingsDialogEvent
    data class AppPassword(val event: AppPasswordDialogEvent) : SettingsDialogEvent
}
