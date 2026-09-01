package com.aozijx.passly.presentation.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataManagementSettingsUiState())
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = DataManagementSettingsUiState(
                    directoryUri = settings.backup.directoryTreeUri,
                )
            }
        }
    }

    fun onAction(action: DataManagementSettingsUiAction) {
        val command = when (action) {
            is DataManagementSettingsUiAction.SetBackupDirectoryUri -> SettingsCommand.SetBackupDirectoryUri(action.uri)
            DataManagementSettingsUiAction.ClearBackupDirectory -> SettingsCommand.ClearBackupDirectoryUri
        }
        viewModelScope.launch { settingsRepository.update(command) }
    }
}
