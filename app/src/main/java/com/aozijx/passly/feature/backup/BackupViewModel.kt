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
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.backup.model.BackupExportOptions
import com.aozijx.passly.domain.backup.model.BackupExportRequest
import com.aozijx.passly.domain.backup.model.BackupImportRequest
import com.aozijx.passly.domain.backup.service.VaultBackupService
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice
import com.aozijx.passly.domain.notice.port.AppNoticePublisher
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.backup.contract.BackupUiState
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val backupService: VaultBackupService,
    private val storageSupport: BackupExportStorageSupport,
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: VaultAccessState,
    private val noticePublisher: AppNoticePublisher
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.CheckDirectoryPermission -> checkDirectoryPermission(intent.uri)
            is BackupIntent.PrepareExport -> prepareExport(intent.format)
            BackupIntent.PrepareRecoveryExport -> prepareRecoveryExport()
            is BackupIntent.StartExport -> startExport(
                uri = intent.uri,
                fileNameHint = intent.fileNameHint,
                deleteOnFailure = intent.deleteOnFailure
            )
            BackupIntent.StartExportInConfiguredDirectory -> startExportInConfiguredDirectory()
            is BackupIntent.StartImport -> startImport(intent.uri)
            is BackupIntent.UpdatePassword -> updatePassword(intent.password)
            is BackupIntent.UpdateImportMode -> updateImportMode(intent.mode)
            is BackupIntent.UpdateIncludeIcons -> updateIncludeIcons(intent.include)
            is BackupIntent.UpdateIncludeAttachments -> updateIncludeAttachments(intent.include)
            is BackupIntent.UpdateIncludeDeleted -> updateIncludeDeleted(intent.include)
            is BackupIntent.UpdateIncludedEntryTypes ->
                updateIncludedEntryTypes(intent.types)
            BackupIntent.CancelPendingOperation -> clearPendingOperation()
            BackupIntent.ProcessBackupAction -> processBackupAction()
        }
    }

    private fun checkDirectoryPermission(uri: String?) {
        if (uri.isNullOrBlank()) {
            fail(BackupFailed("尚未配置备份目录"), BackupOperation.DIRECTORY_CHECK)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            when (val result = backupService.checkDirectoryWritable(uri)) {
                is AppResult.Success -> {
                    noticePublisher.publish(
                        newAppNotice(NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED)
                    )
                    _uiState.update {
                        it.copy(status = BackupOperationStatus.Idle, error = null)
                    }
                }

                is AppResult.Failure ->
                    fail(result.error, BackupOperation.DIRECTORY_CHECK)
            }
        }
    }

    private fun prepareExport(format: BackupExportUiFormat) {
        if (!vaultAccessState.hasFullVaultAccess()) {
            fail(BackupFailed("当前会话不能导出普通备份"), BackupOperation.EXPORT)
            return
        }
        _uiState.update {
            it.copy(
                isExporting = true,
                isRecoveryExport = false,
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

    private fun prepareRecoveryExport() {
        if (!vaultAccessState.isRecoveryMode()) {
            fail(BackupFailed("恢复导出只能在恢复模式中使用"), BackupOperation.EXPORT)
            return
        }
        _uiState.update {
            it.copy(
                isExporting = true,
                isRecoveryExport = true,
                selectedExportFormat = BackupExportUiFormat.ENCRYPTED,
                backupUri = null,
                backupPassword = "",
                includeIcons = true,
                includeAttachments = true,
                includeDeleted = true,
                includedEntryTypes = EntryType.entries.toSet(),
                pendingExportFileName = buildExportFileName(BackupExportUiFormat.ENCRYPTED),
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
        if (!canUseCurrentModeForPendingExport()) return
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

    private fun startExportInConfiguredDirectory() {
        val snapshot = _uiState.value
        if (!snapshot.isExporting || !snapshot.canSubmitExport) return
        if (!canUseMode(snapshot)) return
        viewModelScope.launch {
            if (!authenticate(
                    snapshot.exportAuthenticationPurpose(),
                    BackupOperation.EXPORT
                )
            ) {
                clearPendingOperation()
                return@launch
            }
            _uiState.update { it.copy(status = BackupOperationStatus.Loading, error = null) }
            val directoryUri = settingsRepository.settings.first().backup.directoryTreeUri
            if (directoryUri.isNullOrBlank()) {
                fail(BackupFailed("尚未配置备份目录"), BackupOperation.EXPORT)
                clearPendingFields()
                return@launch
            }
            val fileName = snapshot.pendingExportFileName
                ?: buildExportFileName(snapshot.selectedExportFormat)
            val target = storageSupport.createNamedExportTarget(
                directoryTreeUri = directoryUri,
                fileName = fileName,
                mimeType = snapshot.selectedExportFormat.mimeType
            ).getOrElse { error ->
                fail(
                    AppError.fromThrowable(error, ErrorLayer.UI),
                    BackupOperation.EXPORT
                )
                clearPendingFields()
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
        if (!vaultAccessState.hasFullVaultAccess()) {
            fail(BackupFailed("当前会话不能导入备份"), BackupOperation.IMPORT)
            return
        }
        _uiState.update {
            it.copy(
                isExporting = false,
                isRecoveryExport = false,
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
                snapshot.exportAuthenticationPurpose()
            } else {
                AuthenticationPurpose.BACKUP_IMPORT
            }
            val operation = if (snapshot.isExporting) {
                BackupOperation.EXPORT
            } else {
                BackupOperation.IMPORT
            }
            if (!canUseMode(snapshot, operation)) return@launch
            if (!authenticate(purpose, operation)) {
                clearPendingOperation()
                return@launch
            }
            performOperation(snapshot, targetUri)
        }
    }

    private suspend fun authenticate(
        purpose: AuthenticationPurpose,
        operation: BackupOperation
    ): Boolean =
        when (
            authenticationManager.authenticate(
                AuthenticationRequest(purpose = purpose)
            )
        ) {
            is AuthenticationResult.Success -> true
            is AuthenticationResult.Cancelled -> false
            is AuthenticationResult.Failure -> {
                fail(BackupFailed("身份验证失败"), operation)
                false
            }
        }

    private suspend fun performOperation(state: BackupUiState, targetUri: Uri) {
        if (!canUseMode(state, state.toOperation())) return
        if (state.isRecoveryExport &&
            (state.selectedExportFormat != BackupExportUiFormat.ENCRYPTED ||
                    state.backupPassword.isBlank())
        ) {
            fail(
                BackupFailed("恢复模式只能导出使用新备份密码加密的 Passly 备份"),
                BackupOperation.EXPORT
            )
            clearPendingFields()
            return
        }
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
                    fail(result.error, state.toOperation())
                }
            }
        } catch (error: Exception) {
            if (state.isExporting && state.deleteTargetOnFailure) {
                storageSupport.deleteDocument(targetUri)
            }
            fail(
                AppError.fromThrowable(error, ErrorLayer.UI),
                state.toOperation()
            )
        } finally {
            password?.fill('\u0000')
            clearPendingFields()
        }
    }

    private suspend fun handleSuccess(state: BackupUiState) {
        val code = if (state.isExporting) {
            NoticeCode.BACKUP_EXPORT_COMPLETED
        } else {
            NoticeCode.BACKUP_IMPORT_COMPLETED
        }
        noticePublisher.publish(newAppNotice(code))
        _uiState.update {
            it.copy(status = BackupOperationStatus.Idle, error = null)
        }
    }

    private fun fail(error: AppError, operation: BackupOperation) {
        _uiState.update {
            it.copy(status = BackupOperationStatus.Failure, error = error)
        }
        val code = when (operation) {
            BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
            BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
            BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
        }
        noticePublisher.publish(newAppNotice(code))
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
                deleteTargetOnFailure = false,
                isRecoveryExport = false
            )
        }
    }

    fun buildExportFileName(
        format: BackupExportUiFormat = _uiState.value.selectedExportFormat
    ): String = storageSupport.buildBackupFileName(format.extension)

    private fun BackupUiState.toOperation(): BackupOperation =
        if (isExporting) BackupOperation.EXPORT else BackupOperation.IMPORT

    private fun BackupUiState.exportAuthenticationPurpose(): AuthenticationPurpose =
        if (isRecoveryExport) {
            AuthenticationPurpose.RECOVERY_EXPORT
        } else {
            AuthenticationPurpose.BACKUP_EXPORT
        }

    private fun canUseCurrentModeForPendingExport(): Boolean =
        canUseMode(_uiState.value, BackupOperation.EXPORT)

    private fun canUseMode(
        state: BackupUiState,
        operation: BackupOperation = state.toOperation()
    ): Boolean {
        val allowed = if (state.isRecoveryExport) {
            vaultAccessState.isRecoveryMode()
        } else {
            vaultAccessState.hasFullVaultAccess()
        }
        if (allowed) return true
        fail(
            BackupFailed(
                if (state.isRecoveryExport) {
                    "恢复导出只能在恢复模式中使用"
                } else {
                    "当前会话不能执行备份操作"
                }
            ),
            operation
        )
        clearPendingFields()
        return false
    }

    private enum class BackupOperation {
        EXPORT,
        IMPORT,
        DIRECTORY_CHECK
    }
}
