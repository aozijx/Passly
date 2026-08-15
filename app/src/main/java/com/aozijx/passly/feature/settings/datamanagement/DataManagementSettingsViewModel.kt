package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.model.DatabaseRecoverySelection
import com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authenticationManager: AuthenticationManager,
    private val databaseRecoveryRepository: DatabaseRecoveryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementSettingsUiState())
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                mutate(
                    DataManagementSettingsMutation.SettingsChanged(
                        autoDownloadIcons = settings.interaction.isAutoDownloadIcons,
                        directoryUri = settings.backup.directoryTreeUri,
                    )
                )
            }
        }
        viewModelScope.launch {
            entryListQueryRepository.deletedEntries
                .catch { error ->
                    mutate(
                        DataManagementSettingsMutation.TrashLoadFailed(
                            error.toUiMessage("无法读取回收站")
                        )
                    )
                }
                .collect { entries ->
                    mutate(DataManagementSettingsMutation.TrashLoaded(entries))
                }
        }
        loadRecoveryPackages()
    }

    fun onAction(action: DataManagementSettingsAction) {
        when (action) {
            is DataManagementSettingsAction.SetAutoDownloadIcons ->
                updateSettings(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            is DataManagementSettingsAction.SetBackupDirectoryUri ->
                updateSettings(SettingsCommand.SetBackupDirectoryUri(action.uri))
            is DataManagementSettingsAction.ClearBackupDirectory ->
                updateSettings(SettingsCommand.ClearBackupDirectoryUri)

            is DataManagementSettingsAction.RestoreTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.restoreEntry(
                    id = EntryId(action.entryId),
                    expectedVersion = EntryVersion(action.expectedVersion)
                )
            }

            is DataManagementSettingsAction.DeleteTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.deletePermanently(
                    id = EntryId(action.entryId),
                    expectedVersion = EntryVersion(action.expectedVersion)
                )
            }

            is DataManagementSettingsAction.EmptyTrash -> emptyTrash()

            is DataManagementSettingsAction.ClearTrashError -> {
                mutate(DataManagementSettingsMutation.TrashErrorCleared)
            }
            DataManagementSettingsAction.RefreshRecoveryPackages -> loadRecoveryPackages()
            is DataManagementSettingsAction.ScanRecoveryPackage ->
                scanRecoveryPackage(action.packageId)
            is DataManagementSettingsAction.ToggleRecoveryType ->
                mutate(DataManagementSettingsMutation.RecoveryTypeToggled(action.entryType))
            is DataManagementSettingsAction.RestoreRecoveryPackage ->
                restoreRecoveryPackage(action.packageId)
            is DataManagementSettingsAction.DeleteRecoveryPackage ->
                deleteRecoveryPackage(action.packageId)
            DataManagementSettingsAction.ClearRecoveryResult ->
                mutate(DataManagementSettingsMutation.RecoveryResultCleared)
        }
    }

    private fun loadRecoveryPackages() {
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            runCatching { databaseRecoveryRepository.listPackages() }
                .onSuccess {
                    mutate(DataManagementSettingsMutation.RecoveryPackagesLoaded(it))
                }
                .onFailure {
                    mutate(
                        DataManagementSettingsMutation.RecoveryOperationFailed(
                            it.toUiMessage("无法读取数据库恢复包"),
                        ),
                    )
                }
        }
    }

    private fun scanRecoveryPackage(packageId: String) {
        if (_uiState.value.isRecoveryBusy) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            mutate(DataManagementSettingsMutation.RecoveryOperationStarted(packageId))
            runCatching { databaseRecoveryRepository.scan(packageId) }
                .onSuccess {
                    mutate(DataManagementSettingsMutation.RecoveryScanCompleted(it))
                    refreshRecoveryPackagesAfterOperation()
                }
                .onFailure {
                    mutate(
                        DataManagementSettingsMutation.RecoveryOperationFailed(
                            it.toUiMessage("恢复包预检失败"),
                        ),
                    )
                    refreshRecoveryPackagesAfterOperation()
                }
        }
    }

    private fun restoreRecoveryPackage(packageId: String) {
        val state = _uiState.value
        if (state.isRecoveryBusy || state.recoveryScan?.packageId != packageId ||
            state.selectedRecoveryTypes.isEmpty()
        ) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.RESTORE_DATABASE),
                )
            ) {
                is AuthenticationResult.Success -> {
                    if (!requireRecoveryAccess()) return@launch
                    mutate(DataManagementSettingsMutation.RecoveryOperationStarted(packageId))
                    runCatching {
                        databaseRecoveryRepository.restore(
                            packageId,
                            DatabaseRecoverySelection(state.selectedRecoveryTypes),
                        )
                    }.onSuccess {
                        mutate(DataManagementSettingsMutation.RecoveryRestoreCompleted(it))
                        refreshRecoveryPackagesAfterOperation()
                    }.onFailure {
                        mutate(
                            DataManagementSettingsMutation.RecoveryOperationFailed(
                                it.toUiMessage("数据库恢复失败"),
                            ),
                        )
                    }
                }
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> mutate(
                    DataManagementSettingsMutation.RecoveryOperationFailed("身份验证失败"),
                )
            }
        }
    }

    private fun deleteRecoveryPackage(packageId: String) {
        if (_uiState.value.isRecoveryBusy) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.RESTORE_DATABASE),
                )
            ) {
                is AuthenticationResult.Success -> {
                    mutate(DataManagementSettingsMutation.RecoveryOperationStarted(packageId))
                    runCatching { databaseRecoveryRepository.delete(packageId) }
                        .onSuccess {
                            mutate(DataManagementSettingsMutation.RecoveryResultCleared)
                            refreshRecoveryPackagesAfterOperation()
                        }
                        .onFailure {
                            mutate(
                                DataManagementSettingsMutation.RecoveryOperationFailed(
                                    it.toUiMessage("无法删除数据库恢复包"),
                                ),
                            )
                        }
                }
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> mutate(
                    DataManagementSettingsMutation.RecoveryOperationFailed("身份验证失败"),
                )
            }
        }
    }

    private suspend fun refreshRecoveryPackagesAfterOperation() {
        runCatching { databaseRecoveryRepository.listPackages() }
            .onSuccess { mutate(DataManagementSettingsMutation.RecoveryPackagesLoaded(it)) }
    }

    private fun requireRecoveryAccess(): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        mutate(
            DataManagementSettingsMutation.RecoveryOperationFailed(
                "当前会话不能访问数据库恢复包",
            ),
        )
        return false
    }

    private fun updateSettings(command: SettingsCommand) {
        viewModelScope.launch { settingsRepository.update(command) }
    }

    private fun runTrashEntryAction(
        entryId: String,
        operation: suspend () -> AppResult<Unit>
    ) {
        if (_uiState.value.isTrashBusy) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            mutate(DataManagementSettingsMutation.TrashEntryActionStarted(entryId))
            try {
                operation().updateTrashError("回收站操作失败")
            } finally {
                mutate(DataManagementSettingsMutation.TrashEntryActionFinished)
            }
        }
    }

    private fun emptyTrash() {
        if (_uiState.value.isTrashBusy || _uiState.value.deletedEntries.isEmpty()) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            mutate(DataManagementSettingsMutation.EmptyTrashStarted)
            try {
                entryCommandRepository.emptyTrash().updateTrashError("无法清空回收站")
            } finally {
                mutate(DataManagementSettingsMutation.EmptyTrashFinished)
            }
        }
    }

    private fun requireTrashAccess(): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        mutate(DataManagementSettingsMutation.TrashActionFailed("当前会话不能操作回收站"))
        return false
    }

    private fun AppResult<*>.updateTrashError(fallback: String) {
        if (this is AppResult.Failure) {
            mutate(DataManagementSettingsMutation.TrashActionFailed(error.toUiMessage(fallback)))
        }
    }

    private fun mutate(mutation: DataManagementSettingsMutation) {
        _uiState.value = DataManagementSettingsReducer.reduce(_uiState.value, mutation)
    }
}
