package com.aozijx.passly.presentation.ui.settings.backup.model

internal data class DataManagementDetailState(
    val isAutoDownloadIcons: Boolean,
)

internal interface DataManagementEventHandler {
    fun onAutoDownloadIconsChanged(enabled: Boolean)
    fun onOpenTrash()
    fun onOpenDatabaseRecovery()
}
