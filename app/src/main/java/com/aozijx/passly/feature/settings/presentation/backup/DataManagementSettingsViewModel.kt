package com.aozijx.passly.feature.settings.presentation.backup

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val directorySettings: BackupDirectorySettingsInteractor,
) : ViewModel() {
    private data class PageEffect(val owner: Any, val effect: DataManagementSettingsEffect)

    private val _uiState = MutableStateFlow<DataManagementSettingsUiState>(
        DataManagementSettingsUiState.Loading,
    )
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()
    private val _effects = Channel<PageEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.effect }
    private var activePageOwner: Any? = null

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
        val owner = activePageOwner
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
            BackupDirectorySelectionResult.Cancelled -> emitEffect(
                owner,
                DataManagementSettingsEffect.BackupDirectorySelectionCancelled,
            )
            BackupDirectorySelectionResult.PermissionDenied -> emitEffect(
                owner,
                DataManagementSettingsEffect.BackupDirectoryPermissionDenied,
            )
            BackupDirectorySelectionResult.Unavailable -> emitEffect(
                owner,
                DataManagementSettingsEffect.BackupDirectoryUnavailable,
            )
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }

    private fun emitEffect(owner: Any, effect: DataManagementSettingsEffect) {
        if (activePageOwner === owner) _effects.trySend(PageEffect(owner, effect))
    }
}
