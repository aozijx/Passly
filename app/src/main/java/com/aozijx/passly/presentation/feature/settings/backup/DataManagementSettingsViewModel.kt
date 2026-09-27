package com.aozijx.passly.presentation.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.backup.BackupDirectorySelectionResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val directorySettings: BackupDirectorySettingsInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataManagementSettingsUiState())
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DataManagementSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            directorySettings.directoryUri.collect { directoryUri ->
                _uiState.value = DataManagementSettingsUiState(
                    directoryUri = directoryUri,
                )
            }
        }
    }

    fun onAction(action: DataManagementSettingsUiAction) {
        viewModelScope.launch {
            when (action) {
                is DataManagementSettingsUiAction.BackupDirectoryPicked ->
                    handleSelection(action.uri)
                DataManagementSettingsUiAction.ClearBackupDirectory ->
                    directorySettings.clear(_uiState.value.directoryUri)
            }
        }
    }

    private suspend fun handleSelection(uri: String?) {
        when (directorySettings.select(uri)) {
            BackupDirectorySelectionResult.Saved -> Unit
            BackupDirectorySelectionResult.Cancelled -> _effects.send(
                DataManagementSettingsEffect.BackupDirectorySelectionCancelled,
            )
            BackupDirectorySelectionResult.PermissionDenied -> _effects.send(
                DataManagementSettingsEffect.BackupDirectoryPermissionDenied,
            )
            BackupDirectorySelectionResult.Unavailable -> _effects.send(
                DataManagementSettingsEffect.BackupDirectoryUnavailable,
            )
        }
    }
}
