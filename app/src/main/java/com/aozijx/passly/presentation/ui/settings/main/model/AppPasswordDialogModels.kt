package com.aozijx.passly.presentation.ui.settings.main.model

internal sealed interface AppPasswordDialogState {
    data object None : AppPasswordDialogState
    data object Action : AppPasswordDialogState
    data object Set : AppPasswordDialogState
    data object Change : AppPasswordDialogState
}

internal data class AppPasswordDialogsModel(
    val activeAppPasswordDialog: AppPasswordDialogState,
    val appPasswordCurrent: String,
    val appPasswordNew: String,
    val appPasswordConfirm: String,
    val isSetPasswordConfirmEnabled: Boolean,
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
