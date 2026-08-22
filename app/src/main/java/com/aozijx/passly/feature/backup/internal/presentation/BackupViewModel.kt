package com.aozijx.passly.feature.backup.internal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.contract.BackupUiAction
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BackupViewModel @Inject constructor(
    private val operationCoordinator: BackupOperationCoordinator,
    private val sessionPolicy: BackupSessionPolicy,
    private val noticePublisher: AppNoticePublisher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onAction(action: BackupUiAction) {
        when (action) {
            is BackupUiAction.CheckDirectoryPermission -> checkDirectoryPermission(action.uri)
            is BackupUiAction.PrepareExport -> prepareExport(action.format)
            is BackupUiAction.StartExport -> selectExportTarget(
                uri = action.uri,
                deleteOnFailure = action.deleteOnFailure,
            )

            BackupUiAction.StartExportInConfiguredDirectory -> exportToConfiguredDirectory()
            is BackupUiAction.StartImport -> prepareImport(action.uri)
            is BackupUiAction.UpdatePassword -> replacePassword(action.password)

            is BackupUiAction.UpdateImportMode ->
                mutate(BackupMutation.ImportModeUpdated(action.mode))

            is BackupUiAction.UpdateIncludeIcons ->
                mutate(BackupMutation.IncludeIconsUpdated(action.include))

            is BackupUiAction.UpdateIncludeAttachments ->
                mutate(BackupMutation.IncludeAttachmentsUpdated(action.include))

            is BackupUiAction.UpdateIncludeDeleted ->
                mutate(BackupMutation.IncludeDeletedUpdated(action.include))

            is BackupUiAction.UpdateIncludedEntryTypes ->
                mutate(BackupMutation.IncludedEntryTypesUpdated(action.types))

            BackupUiAction.CancelPendingOperation -> clearPasswordAndMutate(
                BackupMutation.PendingOperationCleared
            )

            BackupUiAction.ProcessBackupAction -> processPendingOperation()
        }
    }

    private fun checkDirectoryPermission(uri: String?) {
        viewModelScope.launch {
            mutate(BackupMutation.OperationStarted)
            applyResult(
                result = operationCoordinator.checkDirectoryWritable(uri),
                operation = BackupOperation.DIRECTORY_CHECK,
                clearPendingFields = false,
            )
        }
    }

    private fun prepareExport(format: BackupExportUiFormat) {
        if (!requireSession(sessionPolicy.regularExportDenial(), BackupOperation.EXPORT)) return
        clearPasswordAndMutate(
            BackupMutation.ExportPrepared(
                format = format,
                fileName = operationCoordinator.buildExportFileName(format),
            ),
        )
    }

    private fun selectExportTarget(
        uri: android.net.Uri,
        deleteOnFailure: Boolean,
    ) {
        if (!canUseCurrentMode(BackupOperation.EXPORT)) return
        mutate(BackupMutation.ExportTargetSelected(uri, deleteOnFailure))
    }

    private fun prepareImport(uri: android.net.Uri) {
        if (!requireSession(sessionPolicy.importDenial(), BackupOperation.IMPORT)) return
        clearPasswordAndMutate(BackupMutation.ImportPrepared(uri))
    }

    private fun exportToConfiguredDirectory() {
        val snapshot = _uiState.value
        if (!snapshot.isExporting || !snapshot.canSubmitExport) return
        if (!canUseMode(snapshot, BackupOperation.EXPORT)) return
        viewModelScope.launch {
            mutate(BackupMutation.OperationStarted)
            applyResult(
                result = operationCoordinator.exportToConfiguredDirectory(snapshot),
                operation = BackupOperation.EXPORT,
                clearPendingFields = true,
            )
        }
    }

    private fun processPendingOperation() {
        val snapshot = _uiState.value
        if (snapshot.backupUri == null) return
        if (snapshot.isExporting && !snapshot.canSubmitExport) return
        val operation = snapshot.operation()
        if (!canUseMode(snapshot, operation)) return
        viewModelScope.launch {
            mutate(BackupMutation.OperationStarted)
            applyResult(
                result = operationCoordinator.executePending(snapshot),
                operation = operation,
                clearPendingFields = true,
            )
        }
    }

    private fun applyResult(
        result: BackupExecutionResult,
        operation: BackupOperation,
        clearPendingFields: Boolean,
    ) {
        when (result) {
            BackupExecutionResult.Success -> {
                noticePublisher.publish(newAppNotice(operation.successNotice()))
                mutate(BackupMutation.OperationSucceeded)
                if (clearPendingFields) {
                    clearPasswordAndMutate(BackupMutation.PendingFieldsCleared)
                }
            }

            BackupExecutionResult.Cancelled ->
                clearPasswordAndMutate(BackupMutation.PendingOperationCleared)

            is BackupExecutionResult.Failure -> {
                noticePublisher.publish(newAppNotice(operation.failureNotice()))
                mutate(BackupMutation.OperationFailed(result.error))
                if (clearPendingFields) {
                    clearPasswordAndMutate(BackupMutation.PendingFieldsCleared)
                }
            }
        }
    }

    private fun canUseCurrentMode(operation: BackupOperation): Boolean =
        canUseMode(_uiState.value, operation)

    private fun canUseMode(state: BackupUiState, operation: BackupOperation): Boolean {
        if (requireSession(sessionPolicy.pendingOperationDenial(), operation)) return true
        clearPasswordAndMutate(BackupMutation.PendingFieldsCleared)
        return false
    }

    private fun requireSession(
        denial: BackupSessionDenial?,
        operation: BackupOperation,
    ): Boolean {
        if (denial == null) return true
        noticePublisher.publish(newAppNotice(operation.failureNotice()))
        mutate(BackupMutation.OperationFailed(BackupFailed()))
        return false
    }

    private fun mutate(mutation: BackupMutation) {
        _uiState.update { state -> BackupReducer.reduce(state, mutation) }
    }

    private fun replacePassword(password: SensitiveValue) {
        val previous = _uiState.value.backupPassword
        mutate(BackupMutation.PasswordUpdated(password))
        if (previous !== password) previous.wipe()
    }

    private fun clearPasswordAndMutate(mutation: BackupMutation) {
        val password = _uiState.value.backupPassword
        mutate(mutation)
        password.wipe()
    }

    override fun onCleared() {
        _uiState.value.backupPassword.wipe()
    }
}

private fun BackupUiState.operation(): BackupOperation =
    if (isExporting) BackupOperation.EXPORT else BackupOperation.IMPORT

private fun BackupOperation.successNotice(): NoticeCode = when (this) {
    BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_COMPLETED
    BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_COMPLETED
    BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED
}

private fun BackupOperation.failureNotice(): NoticeCode = when (this) {
    BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
    BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
    BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
}
