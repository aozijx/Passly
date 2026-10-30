package com.aozijx.passly.feature.settings.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.backup.BackupDirectorySelectionResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val directorySettings: BackupDirectorySettingsInteractor,
) : ViewModel() {
    private val pageEffects = PageEffects<DataManagementSettingsEffect>()

    private val _uiState = MutableStateFlow<DataManagementSettingsUiState>(
        DataManagementSettingsUiState.Loading,
    )
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()
    val effects = pageEffects.effects

    init {
        viewModelScope.launch {
            directorySettings.directoryUri.collect { directoryUri ->
                _uiState.value = DataManagementSettingsUiState.Ready(
                    directoryUri = directoryUri,
                )
            }
        }
    }

    fun onAction(action: DataManagementSettingsUiAction) {
        val owner = pageEffects.currentOwner
        viewModelScope.launch {
            when (action) {
                is DataManagementSettingsUiAction.BackupDirectoryPicked ->
                    owner?.let { handleSelection(action.uri, it) }
                DataManagementSettingsUiAction.ClearBackupDirectory -> when (_uiState.value) {
                    DataManagementSettingsUiState.Loading -> Unit
                    is DataManagementSettingsUiState.Ready -> directorySettings.clear()
                }
            }
        }
    }

    private suspend fun handleSelection(uri: String?, owner: Any) {
        when (directorySettings.select(uri)) {
            BackupDirectorySelectionResult.Saved -> Unit
            BackupDirectorySelectionResult.Cancelled -> pageEffects.send(
                owner,
                DataManagementSettingsEffect.BackupDirectorySelectionCancelled,
            )
            BackupDirectorySelectionResult.PermissionDenied -> pageEffects.send(
                owner,
                DataManagementSettingsEffect.BackupDirectoryPermissionDenied,
            )
            BackupDirectorySelectionResult.Unavailable -> pageEffects.send(
                owner,
                DataManagementSettingsEffect.BackupDirectoryUnavailable,
            )
        }
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }
}
