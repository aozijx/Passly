package com.aozijx.passly.feature.settings.presentation.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.settings.backup.BackupDirectoryStatusReader
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
class PermissionManagementViewModel
    @Inject
    constructor(
        private val directorySettings: BackupDirectorySettingsSource,
        private val directoryStatusReader: BackupDirectoryStatusReader,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PermissionManagementUiState())
        val uiState: StateFlow<PermissionManagementUiState> = _uiState.asStateFlow()

        private var runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess> = emptyMap()
        private var directoryUri: String? = null
        private var directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED
        private var directoryReadState: RecoverableReadState<String?> = RecoverableReadState.Loading
        private val directoryRead = RecoverableRead(directorySettings.backupDirectoryUri, viewModelScope)

        init {
            viewModelScope.launch {
                directoryRead.state.collectLatest { read ->
                    directoryReadState = read
                    if (read is RecoverableReadState.Ready) {
                        directoryUri = read.value
                        directoryStatus = inspectDirectory(read.value)
                    } else {
                        directoryUri = null
                        directoryStatus = PersistedDirectoryAccessStatus.UNAVAILABLE
                    }
                    publishState()
                }
            }
        }

        fun runtimeAccessUpdated(access: Map<RuntimePermission, RuntimePermissionAccess>) {
            runtimeAccess = access.toMap()
            publishState()
        }

        fun refreshDirectoryAccess() {
            if (directoryReadState !is RecoverableReadState.Ready) {
                directoryRead.retry()
                return
            }
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
            withContext(Dispatchers.IO) { directoryStatusReader.inspect(uri) }

        private fun publishState() {
            _uiState.value =
                PermissionManagementUiMapper
                    .map(
                        runtimeAccess = runtimeAccess,
                        directoryStatus = directoryStatus,
                    ).copy(backupDirectoryReadState = directoryReadState)
        }

        fun retryBackupDirectoryRead() {
            directoryRead.retry()
        }
    }
