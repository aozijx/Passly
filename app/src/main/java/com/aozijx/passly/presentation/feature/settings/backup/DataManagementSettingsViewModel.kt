package com.aozijx.passly.presentation.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: BackupDirectorySettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataManagementSettingsUiState())
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.backupDirectoryUri.collect { directoryUri ->
                _uiState.value = DataManagementSettingsUiState(
                    directoryUri = directoryUri,
                )
            }
        }
    }

    fun onAction(action: DataManagementSettingsUiAction) {
        viewModelScope.launch {
            when (action) {
                is DataManagementSettingsUiAction.SetBackupDirectoryUri ->
                    settingsRepository.setBackupDirectoryUri(action.uri)
                DataManagementSettingsUiAction.ClearBackupDirectory ->
                    settingsRepository.clearBackupDirectoryUri()
            }
        }
    }
}
