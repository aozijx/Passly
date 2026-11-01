package com.aozijx.passly.feature.backup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.operation.BackupExecutionResult
import com.aozijx.passly.feature.backup.internal.operation.BackupExportOperationRequest
import com.aozijx.passly.feature.backup.internal.operation.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class BackupViewModel @Inject constructor(
    private val backupOperationInteractor: BackupOperationInteractor,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val pageEffects = PageEffects<BackupEffect>()
    val effects = pageEffects.effects
    private var pendingImportDocument: PendingImportDocument? = null

    fun onAction(action: BackupUiAction) {
        if (_uiState.value.workflow is BackupWorkflow.Executing) return
        when (action) {
            is BackupUiAction.CheckDirectoryPermission -> checkDirectoryPermission(action.uri)
            BackupUiAction.OpenExportOptions -> mutate(BackupMutation.ExportOptionsOpened)
            is BackupUiAction.RequestImportDocument -> requestImportDocument(action.pageOwner)
            is BackupUiAction.SelectExportFormat -> prepareExport(action.format)
            is BackupUiAction.StartExport -> startManualExport(
                uri = action.uri,
                deleteOnFailure = action.deleteOnFailure,
            )
            is BackupUiAction.StartImport -> startImport(action)
            is BackupUiAction.UpdatePassword -> replacePassword(
                OwnedChars.fromString(action.password),
            )
            is BackupUiAction.UpdateImportMode ->
                mutate(BackupMutation.ImportModeUpdated(action.mode))
            is BackupUiAction.UpdateImportStrategy ->
                mutate(BackupMutation.ImportStrategyUpdated(action.strategy))
            is BackupUiAction.UpdateIncludeIcons ->
                mutate(BackupMutation.IncludeIconsUpdated(action.include))
            is BackupUiAction.UpdateIncludeAttachments ->
                mutate(BackupMutation.IncludeAttachmentsUpdated(action.include))
            is BackupUiAction.UpdateIncludeDeleted ->
                mutate(BackupMutation.IncludeDeletedUpdated(action.include))
            is BackupUiAction.UpdateIncludedEntryTypes -> mutate(
                BackupMutation.IncludedEntryTypesUpdated(action.types),
            )
            is BackupUiAction.SubmitExport -> submitExport(action.useConfiguredDirectory)
            BackupUiAction.SubmitImport -> submitImport()
            BackupUiAction.DismissOptions -> closeWorkflow()
        }
    }

    private fun checkDirectoryPermission(uri: String?) {
        if (_uiState.value.workflow is BackupWorkflow.Executing) return
        execute(
            operation = BackupOperation.DIRECTORY_CHECK,
            password = EmptySensitiveValue,
        ) {
            backupOperationInteractor.checkDirectoryWritable(uri)
        }
    }

    private fun prepareExport(format: BackupExportFormat) {
        if (!requireSession(BackupOperation.EXPORT)) return
        replaceWorkflow(
            BackupMutation.ExportPrepared(
                format = format,
                fileName = backupOperationInteractor.buildExportFileName(format),
            ),
        )
    }

    private fun prepareImport(uri: String) {
        if (!requireSession(BackupOperation.IMPORT)) return
        replaceWorkflow(BackupMutation.ImportPrepared(uri))
    }

    private fun requestImportDocument(owner: Any) {
        if (!pageEffects.isActive(owner)) return
        val request = PendingImportDocument(owner, UUID.randomUUID().toString())
        pendingImportDocument = request
        emitEffect(owner, BackupEffect.SelectImportDocument(request.requestId))
    }

    private fun startImport(action: BackupUiAction.StartImport) {
        val pending = pendingImportDocument ?: return
        if (pending.pageOwner !== action.pageOwner || pending.requestId != action.requestId) return
        pendingImportDocument = null
        if (!pageEffects.isActive(action.pageOwner)) return
        action.uri?.let(::prepareImport)
    }

    private fun submitExport(useConfiguredDirectory: Boolean) {
        val workflow = _uiState.value.workflow as? BackupWorkflow.ConfigureExport ?: return
        val options = workflow.options
        if (!options.canSubmit) return
        if (!canExecutePending(BackupOperation.EXPORT)) return
        if (useConfiguredDirectory) {
            execute(
                operation = BackupOperation.EXPORT,
                password = options.password,
            ) {
                backupOperationInteractor.exportToConfiguredDirectory(options.toRequest())
            }
        } else {
            mutate(BackupMutation.ExportTargetRequested)
            emitEffect(
                BackupEffect.SelectExportDocument(
                    format = options.format,
                    fileName = options.fileName,
                ),
            )
        }
    }

    private fun startManualExport(uri: String, deleteOnFailure: Boolean) {
        val workflow = _uiState.value.workflow as? BackupWorkflow.AwaitingExportTarget ?: return
        val options = workflow.options
        if (!options.canSubmit || !canExecutePending(BackupOperation.EXPORT)) return
        execute(
            operation = BackupOperation.EXPORT,
            password = options.password,
        ) {
            backupOperationInteractor.exportToUri(
                request = options.toRequest(),
                targetUri = uri,
                deleteOnFailure = deleteOnFailure,
            )
        }
    }

    private fun submitImport() {
        val workflow = _uiState.value.workflow as? BackupWorkflow.ConfigureImport ?: return
        if (!canExecutePending(BackupOperation.IMPORT)) return
        val options = workflow.options
        execute(
            operation = BackupOperation.IMPORT,
            password = options.password,
        ) {
            backupOperationInteractor.import(options.toRequest())
        }
    }

    private fun execute(
        operation: BackupOperation,
        password: SensitiveValue,
        block: suspend () -> BackupExecutionResult,
    ) {
        val owner = pageEffects.currentOwner ?: run {
            password.wipe()
            return
        }
        mutate(BackupMutation.OperationStarted(operation))
        viewModelScope.launch {
            try {
                applyResult(block(), operation, owner)
            } finally {
                password.wipe()
            }
        }
    }

    private fun applyResult(
        result: BackupExecutionResult,
        operation: BackupOperation,
        owner: Any,
    ) {
        when (result) {
            BackupExecutionResult.DirectoryWritable -> {
                emitEffect(owner, BackupNoticeEffect.DirectoryWritable)
                mutateIfExecuting(operation, BackupMutation.OperationSucceeded)
            }
            BackupExecutionResult.ExportSucceeded -> {
                emitEffect(owner, BackupNoticeEffect.ExportSucceeded)
                mutateIfExecuting(operation, BackupMutation.OperationSucceeded)
            }
            is BackupExecutionResult.ImportSucceeded -> {
                emitEffect(owner, BackupNoticeEffect.ImportSucceeded(result.result))
                mutateIfExecuting(operation, BackupMutation.OperationSucceeded)
            }
            BackupExecutionResult.Cancelled ->
                mutateIfExecuting(operation, BackupMutation.WorkflowClosed)
            is BackupExecutionResult.Failure -> {
                emitEffect(owner, BackupNoticeEffect.Failed(operation))
                mutateIfExecuting(operation, BackupMutation.OperationFailed(operation, result.error))
            }
        }
    }

    private fun canExecutePending(operation: BackupOperation): Boolean {
        if (requireSession(operation)) return true
        return false
    }

    private fun requireSession(operation: BackupOperation): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        emitEffect(BackupNoticeEffect.Failed(operation))
        replaceWorkflow(BackupMutation.OperationFailed(operation, BackupFailed()))
        return false
    }

    private fun replacePassword(password: SensitiveValue) {
        val previous = _uiState.value.workflow.password()
        mutate(BackupMutation.PasswordUpdated(password))
        val accepted = _uiState.value.workflow.password()
        if (accepted !== password) password.wipe()
        if (previous !== accepted) previous.wipe()
    }

    private fun replaceWorkflow(mutation: BackupMutation) {
        val previous = _uiState.value.workflow.password()
        mutate(mutation)
        val current = _uiState.value.workflow.password()
        if (previous !== current) previous.wipe()
    }

    private fun closeWorkflow() {
        replaceWorkflow(BackupMutation.WorkflowClosed)
    }

    private fun emitEffect(effect: BackupEffect) {
        pageEffects.tryEmit(effect)
    }

    private fun emitEffect(owner: Any, effect: BackupEffect) {
        pageEffects.tryEmit(owner, effect)
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        if (!pageEffects.isActive(owner)) return
        pageEffects.exit(owner)
        if (pendingImportDocument?.pageOwner === owner) pendingImportDocument = null
        if (_uiState.value.workflow !is BackupWorkflow.Executing) closeWorkflow()
    }

    private fun mutate(mutation: BackupMutation) {
        _uiState.update { state -> BackupReducer.reduce(state, mutation) }
    }

    private fun mutateIfExecuting(operation: BackupOperation, mutation: BackupMutation) {
        if (_uiState.value.workflow == BackupWorkflow.Executing(operation)) mutate(mutation)
    }

    override fun onCleared() {
        pendingImportDocument = null
        _uiState.value.workflow.password().wipe()
    }

    private data class PendingImportDocument(val pageOwner: Any, val requestId: String)
}

private fun BackupExportOptions.toRequest() = BackupExportOperationRequest(
    format = format,
    includeIcons = includeIcons,
    includeAttachments = includeAttachments,
    includeDeleted = includeDeleted,
    includedEntryTypes = includedEntryTypes,
    password = password,
    fileName = fileName,
)

private fun BackupImportOptions.toRequest() = BackupImportOperationRequest(
    mode = mode,
    strategy = strategy,
    sourceUri = sourceUri,
    password = password,
)
