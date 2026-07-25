package com.aozijx.passly.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.BackupFailed
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.fromThrowable
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.backup.model.BackupExportOptions
import com.aozijx.passly.domain.backup.model.BackupExportRequest
import com.aozijx.passly.domain.backup.model.BackupImportRequest
import com.aozijx.passly.domain.backup.service.VaultBackupService
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.backup.contract.BackupEffect
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.backup.contract.BackupUiState
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val backupService: VaultBackupService,
    private val storageSupport: BackupExportStorageSupport,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _effect = Channel<BackupEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.CheckDirectoryPermission -> checkDirectoryPermission(intent.uri)
            is BackupIntent.SetBackupDirectoryUri -> setBackupDirectoryUri(intent.uri)
            BackupIntent.ClearBackupDirectoryUri -> clearBackupDirectoryUri()
            is BackupIntent.PrepareExport -> prepareExport(intent.format)
            is BackupIntent.StartExport -> startExport(
                uri = intent.uri,
                fileNameHint = intent.fileNameHint,
                deleteOnFailure = intent.deleteOnFailure
            )
            is BackupIntent.StartExportInConfiguredDirectory ->
                startExportInConfiguredDirectory(intent.directoryUri)
            is BackupIntent.StartImport -> startImport(intent.uri)
            is BackupIntent.UpdatePassword -> updatePassword(intent.password)
            is BackupIntent.UpdateImportMode -> updateImportMode(intent.mode)
            is BackupIntent.UpdateIncludeIcons -> updateIncludeIcons(intent.include)
            is BackupIntent.UpdateIncludeAttachments -> updateIncludeAttachments(intent.include)
            is BackupIntent.UpdateIncludeDeleted -> updateIncludeDeleted(intent.include)
            is BackupIntent.UpdateIncludedEntryTypes ->
                updateIncludedEntryTypes(intent.types)
            BackupIntent.CancelPendingOperation -> clearPendingOperation()
            BackupIntent.ResetBackupStatus ->
                _uiState.update { it.copy(status = BackupOperationStatus.Idle, error = null) }
            BackupIntent.ProcessBackupAction -> processBackupAction()
        }
    }

    private fun checkDirectoryPermission(uri: String?) {
        if (uri.isNullOrBlank()) {
            fail(BackupFailed("尚未配置备份目录"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            when (val result = backupService.checkDirectoryWritable(uri)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        status = BackupOperationStatus.Success(
                            BackupOperationStatus.OperationType.PERMISSION_CHECK
                        ),
                        error = null
                    )
                }
                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun setBackupDirectoryUri(uri: String) {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetBackupDirectoryUri(uri))
        }
    }

    private fun clearBackupDirectoryUri() {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.ClearBackupDirectoryUri())
        }
    }

    private fun prepareExport(format: BackupExportUiFormat) {
        _uiState.update {
            it.copy(
                isExporting = true,
                selectedExportFormat = format,
                backupUri = null,
                backupPassword = "",
                includeIcons = format.supportsResources,
                includeAttachments = format.supportsResources,
                includeDeleted = true,
                includedEntryTypes = EntryType.entries.toSet(),
                pendingExportFileName = buildExportFileName(format),
                deleteTargetOnFailure = false,
                status = BackupOperationStatus.Idle,
                error = null
            )
        }
    }

    private fun startExport(
        uri: Uri,
        fileNameHint: String?,
        deleteOnFailure: Boolean
    ) {
        _uiState.update {
            it.copy(
                isExporting = true,
                backupUri = uri,
                pendingExportFileName =
                    fileNameHint ?: buildExportFileName(it.selectedExportFormat),
                deleteTargetOnFailure = deleteOnFailure
            )
        }
    }

    private fun startExportInConfiguredDirectory(directoryUri: String) {
        val snapshot = _uiState.value
        if (!snapshot.isExporting || !snapshot.canSubmitExport) return
        viewModelScope.launch {
            if (!authenticate(AuthenticationPurpose.BACKUP_EXPORT)) return@launch
            _uiState.update { it.copy(status = BackupOperationStatus.Loading, error = null) }
            val fileName = snapshot.pendingExportFileName
                ?: buildExportFileName(snapshot.selectedExportFormat)
            val target = storageSupport.createNamedExportTarget(
                directoryTreeUri = directoryUri,
                fileName = fileName,
                mimeType = snapshot.selectedExportFormat.mimeType
            ).getOrElse { error ->
                fail(AppError.fromThrowable(error, ErrorLayer.UI))
                return@launch
            }
            performOperation(
                state = snapshot.copy(
                    backupUri = target.fileUri,
                    pendingExportFileName = target.fileName,
                    deleteTargetOnFailure = true
                ),
                targetUri = target.fileUri
            )
        }
    }

    private fun startImport(uri: Uri) {
        _uiState.update {
            it.copy(
                isExporting = false,
                backupUri = uri,
                backupPassword = "",
                importMode = com.aozijx.passly.domain.backup.model.ImportMode.APPEND,
                pendingExportFileName = null,
                deleteTargetOnFailure = false,
                status = BackupOperationStatus.Idle,
                error = null
            )
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(backupPassword = password) }
    }

    private fun updateImportMode(mode: com.aozijx.passly.domain.backup.model.ImportMode) {
        _uiState.update { it.copy(importMode = mode) }
    }

    private fun updateIncludeIcons(include: Boolean) {
        _uiState.update { it.copy(includeIcons = include) }
    }

    private fun updateIncludeAttachments(include: Boolean) {
        _uiState.update { it.copy(includeAttachments = include) }
    }

    private fun updateIncludeDeleted(include: Boolean) {
        _uiState.update { it.copy(includeDeleted = include) }
    }

    private fun updateIncludedEntryTypes(types: Set<EntryType>) {
        _uiState.update { it.copy(includedEntryTypes = types) }
    }

    private fun processBackupAction() {
        val snapshot = _uiState.value
        val targetUri = snapshot.backupUri ?: return
        if (snapshot.isExporting && !snapshot.canSubmitExport) return
        viewModelScope.launch {
            val purpose = if (snapshot.isExporting) {
                AuthenticationPurpose.BACKUP_EXPORT
            } else {
                AuthenticationPurpose.BACKUP_IMPORT
            }
            if (!authenticate(purpose)) return@launch
            performOperation(snapshot, targetUri)
        }
    }

    private suspend fun authenticate(purpose: AuthenticationPurpose): Boolean =
        when (
            authenticationManager.authenticate(
                AuthenticationRequest(purpose = purpose)
            )
        ) {
            is AuthenticationResult.Success -> true
            is AuthenticationResult.Cancelled -> false
            is AuthenticationResult.Failure -> {
                fail(BackupFailed("身份验证失败"))
                false
            }
        }

    private suspend fun performOperation(state: BackupUiState, targetUri: Uri) {
        _uiState.update { it.copy(status = BackupOperationStatus.Loading, error = null) }
        val password = state.backupPassword
            .takeIf(String::isNotEmpty)
            ?.toCharArray()
        try {
            val result = if (state.isExporting) {
                backupService.export(
                    BackupExportRequest(
                        targetUri = targetUri.toString(),
                        format = state.selectedExportFormat.formatId,
                        password = password,
                        options = BackupExportOptions(
                            includeIcons =
                                state.includeIcons && state.selectedExportFormat.supportsResources,
                            includeAttachments =
                                state.includeAttachments &&
                                    state.selectedExportFormat.supportsResources,
                            includeDeleted = state.includeDeleted,
                            includedEntryTypes = state.includedEntryTypes
                        )
                    )
                )
            } else {
                backupService.import(
                    BackupImportRequest(
                        sourceUri = targetUri.toString(),
                        mode = state.importMode,
                        format = null,
                        password = password
                    )
                )
            }
            when (result) {
                is AppResult.Success -> handleSuccess(state)
                is AppResult.Failure -> {
                    if (state.isExporting && state.deleteTargetOnFailure) {
                        storageSupport.deleteDocument(targetUri)
                    }
                    fail(result.error)
                }
            }
        } catch (error: Exception) {
            if (state.isExporting && state.deleteTargetOnFailure) {
                storageSupport.deleteDocument(targetUri)
            }
            fail(AppError.fromThrowable(error, ErrorLayer.UI))
        } finally {
            password?.fill('\u0000')
            clearPendingFields()
        }
    }

    private suspend fun handleSuccess(state: BackupUiState) {
        if (state.isExporting) {
            state.pendingExportFileName?.let { fileName ->
                settingsRepository.update(
                    SettingsCommand.SetLastBackupExportFileName(fileName)
                )
            }
        }
        _uiState.update {
            it.copy(
                status = BackupOperationStatus.Success(
                    if (state.isExporting) {
                        BackupOperationStatus.OperationType.EXPORT
                    } else {
                        BackupOperationStatus.OperationType.IMPORT
                    }
                ),
                error = null
            )
        }
    }

    private fun fail(error: AppError) {
        _uiState.update {
            it.copy(status = BackupOperationStatus.Failure, error = error)
        }
        _effect.trySend(BackupEffect.ShowError(error))
    }

    private fun clearPendingOperation() {
        clearPendingFields()
        _uiState.update { it.copy(status = BackupOperationStatus.Idle, error = null) }
    }

    private fun clearPendingFields() {
        _uiState.update {
            it.copy(
                backupUri = null,
                backupPassword = "",
                pendingExportFileName = null,
                deleteTargetOnFailure = false
            )
        }
    }

    fun buildExportFileName(
        format: BackupExportUiFormat = _uiState.value.selectedExportFormat
    ): String = storageSupport.buildBackupFileName(format.extension)
}
