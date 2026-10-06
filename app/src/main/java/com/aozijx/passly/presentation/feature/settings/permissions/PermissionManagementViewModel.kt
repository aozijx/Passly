package com.aozijx.passly.presentation.feature.settings.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.feature.settings.backup.BackupDirectorySelectionResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PermissionManagementViewModel @Inject constructor(
    private val directorySettings: BackupDirectorySettingsInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionManagementUiState())
    val uiState: StateFlow<PermissionManagementUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PermissionManagementEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess> = emptyMap()
    private var directoryUri: String? = null
    private var directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED

    init {
        viewModelScope.launch {
            directorySettings.directoryUri.collectLatest { uri ->
                directoryUri = uri
                directoryStatus = inspectDirectory(uri)
                publishState()
            }
        }
    }

    fun runtimeAccessUpdated(access: Map<RuntimePermission, RuntimePermissionAccess>) {
        runtimeAccess = access.toMap()
        publishState()
    }

    fun refreshDirectoryAccess() {
        viewModelScope.launch {
            val inspectedUri = directoryUri
            val status = inspectDirectory(inspectedUri)
            if (directoryUri == inspectedUri) {
                directoryStatus = status
                publishState()
            }
        }
    }

    fun directoryPicked(uri: String?) {
        viewModelScope.launch {
            when (directorySettings.select(uri)) {
                BackupDirectorySelectionResult.Saved -> refreshDirectoryAccess()
                BackupDirectorySelectionResult.Cancelled -> _effects.send(
                    PermissionManagementEffect.BackupDirectorySelectionCancelled,
                )
                BackupDirectorySelectionResult.PermissionDenied -> _effects.send(
                    PermissionManagementEffect.BackupDirectoryPermissionDenied,
                )
                BackupDirectorySelectionResult.Unavailable -> _effects.send(
                    PermissionManagementEffect.BackupDirectoryUnavailable,
                )
            }
        }
    }

    fun clearDirectory() {
        viewModelScope.launch {
            directorySettings.clear(directoryUri)
        }
    }

    private suspend fun inspectDirectory(uri: String?): PersistedDirectoryAccessStatus =
        withContext(Dispatchers.IO) { directorySettings.inspect(uri) }

    private fun publishState() {
        _uiState.value = PermissionManagementUiMapper.map(
            runtimeAccess = runtimeAccess,
            directoryUri = directoryUri,
            directoryStatus = directoryStatus,
        )
    }
}
