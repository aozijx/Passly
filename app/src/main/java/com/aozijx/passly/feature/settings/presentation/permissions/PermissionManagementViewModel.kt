package com.aozijx.passly.feature.settings.presentation.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.feature.settings.backup.BackupDirectoryStatusInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PermissionManagementViewModel @Inject constructor(
    private val directoryAccessStatus: BackupDirectoryStatusInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionManagementUiState())
    val uiState: StateFlow<PermissionManagementUiState> = _uiState.asStateFlow()

    private var runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess> = emptyMap()
    private var directoryUri: String? = null
    private var directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED

    init {
        viewModelScope.launch {
            directoryAccessStatus.directoryUri.collectLatest { uri ->
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

    private suspend fun inspectDirectory(uri: String?): PersistedDirectoryAccessStatus =
        withContext(Dispatchers.IO) { directoryAccessStatus.inspect(uri) }

    private fun publishState() {
        _uiState.value = PermissionManagementUiMapper.map(
            runtimeAccess = runtimeAccess,
            directoryUri = directoryUri,
            directoryStatus = directoryStatus,
        )
    }
}
