package com.aozijx.passly.ui.features.settings.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.ui.features.backup.BackupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val lastExportFileName: String? = null,
)

sealed interface DataUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataUiAction
    data object ClearBackupDirectory : DataUiAction
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases,
    private val backupSettingsUseCases: BackupSettingsUseCases,
    val backupCoordinator: BackupCoordinator
) : ViewModel() {

    val config: StateFlow<DataUiState> = combine(
        systemSettingsUseCases.isAutoDownloadIcons,
        backupSettingsUseCases.backupDirectoryUri,
        backupSettingsUseCases.lastBackupExportFileName
    ) { adi, bdu, lef ->
        DataUiState(
            isAutoDownloadIcons = adi,
            directoryUri = bdu,
            lastExportFileName = lef,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        DataUiState()
    )

    fun onAction(action: DataUiAction) {
        when (action) {
            is DataUiAction.SetAutoDownloadIcons -> viewModelScope.launch {
                systemSettingsUseCases.setAutoDownloadIcons(action.enabled)
            }

            is DataUiAction.SetBackupDirectoryUri -> viewModelScope.launch {
                backupSettingsUseCases.setBackupDirectoryUri(action.uri)
            }

            is DataUiAction.ClearBackupDirectory -> viewModelScope.launch {
                backupSettingsUseCases.clearBackupDirectoryUri()
            }
        }
    }

    fun testBackupDirectoryWritePermission(uri: String?) =
        backupCoordinator.testBackupDirectoryWritePermission(uri)
}