package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import com.aozijx.passly.feature.backup.BackupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val lastExportFileName: String? = null,
    val backupMessage: String? = null,
)

sealed interface DataUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataUiAction
    data object ClearBackupDirectory : DataUiAction
    data object ClearBackupMessage : DataUiAction
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val portableSettingsUseCases: PortableSettingsUseCases,
    private val deviceSettingsUseCases: DeviceSettingsUseCases,
    private val backupCoordinator: BackupCoordinator
) : ViewModel() {

    private val _config = MutableStateFlow(DataUiState())
    val config: StateFlow<DataUiState> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                portableSettingsUseCases.isAutoDownloadIcons,
                deviceSettingsUseCases.backupDirectoryUri,
                deviceSettingsUseCases.lastBackupExportFileName,
                snapshotFlow { backupCoordinator.backupMessage }
            ) { adi, bdu, lef, bm ->
                DataUiState(
                    isAutoDownloadIcons = adi,
                    directoryUri = bdu,
                    lastExportFileName = lef,
                    backupMessage = bm,
                )
            }.collect { _config.value = it }
        }
    }

    fun onAction(action: DataUiAction) {
        when (action) {
            is DataUiAction.SetAutoDownloadIcons -> viewModelScope.launch {
                portableSettingsUseCases.setAutoDownloadIcons(action.enabled)
            }

            is DataUiAction.SetBackupDirectoryUri -> viewModelScope.launch {
                deviceSettingsUseCases.setBackupDirectoryUri(action.uri)
            }

            is DataUiAction.ClearBackupDirectory -> viewModelScope.launch {
                deviceSettingsUseCases.clearBackupDirectoryUri()
            }

            is DataUiAction.ClearBackupMessage -> backupCoordinator.clearBackupMessage()
        }
    }

    fun testBackupDirectoryWritePermission(uri: String?) =
        backupCoordinator.testBackupDirectoryWritePermission(uri)
}
