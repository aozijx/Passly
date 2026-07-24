package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.command.settings.SettingsCommand
import com.aozijx.passly.domain.repository.settings.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val lastExportFileName: String? = null
)

sealed interface DataUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataUiAction
    data object ClearBackupDirectory : DataUiAction
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _config = MutableStateFlow(DataUiState())
    val config: StateFlow<DataUiState> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings.map { it.interaction.isAutoDownloadIcons },
                settingsRepository.settings.map { it.backup.backupDirectoryUri },
                settingsRepository.settings.map { it.backup.lastBackupExportFileName }
            ) { adi, bdu, lef ->
                DataUiState(
                    isAutoDownloadIcons = adi,
                    directoryUri = bdu,
                    lastExportFileName = lef
                )
            }.collect { _config.value = it }
        }
    }

    fun onAction(action: DataUiAction) {
        when (action) {
            is DataUiAction.SetAutoDownloadIcons -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            }

            is DataUiAction.SetBackupDirectoryUri -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetBackupDirectoryUri(action.uri))
            }

            is DataUiAction.ClearBackupDirectory -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.ClearBackupDirectoryUri())
            }
        }
    }
}
