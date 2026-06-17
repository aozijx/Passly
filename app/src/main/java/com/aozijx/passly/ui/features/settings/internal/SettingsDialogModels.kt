package com.aozijx.passly.ui.features.settings.internal

import android.content.Context
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType

internal sealed interface AppPasswordDialogState {
    data object None : AppPasswordDialogState
    data object Action : AppPasswordDialogState
    data object Set : AppPasswordDialogState
    data object Change : AppPasswordDialogState
    data object Disable : AppPasswordDialogState
}

internal data class SettingsDialogsState(
    val showRightActionDialog: Boolean,
    val showLeftActionDialog: Boolean,
    val showLockTimeoutDialog: Boolean,
    val showClearBackupDirConfirmDialog: Boolean,
    val showDeviceCredentialFallbackWarningDialog: Boolean,
    val activeAppPasswordDialog: AppPasswordDialogState,
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

internal sealed interface SettingsDialogEvent {
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsDialogEvent
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsDialogEvent
    data class SetLockTimeout(val timeoutMs: Long) : SettingsDialogEvent
    data object ClearBackupDirectory : SettingsDialogEvent
    data object DismissRightActionDialog : SettingsDialogEvent
    data object DismissLeftActionDialog : SettingsDialogEvent
    data object DismissLockTimeoutDialog : SettingsDialogEvent
    data object DismissClearBackupDirConfirmDialog : SettingsDialogEvent
    data object DismissDeviceCredentialFallbackWarningDialog : SettingsDialogEvent
    data object ConfirmEnableDeviceCredentialFallback : SettingsDialogEvent
    data class AppPassword(val event: AppPasswordDialogEvent) : SettingsDialogEvent
}

internal data class SettingsDialogsActions(
    val onDialogEvent: (SettingsDialogEvent) -> Unit
)