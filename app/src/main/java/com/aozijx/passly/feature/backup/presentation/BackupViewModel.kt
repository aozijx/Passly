package com.aozijx.passly.feature.backup.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.operation.BackupExecutionResult
import com.aozijx.passly.feature.backup.internal.operation.BackupExportOperationRequest
import com.aozijx.passly.feature.backup.internal.operation.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor
import com.aozijx.passly.feature.backup.internal.presentation.BackupSessionDenial
import com.aozijx.passly.feature.backup.internal.presentation.BackupSessionPolicy
import com.aozijx.passly.presentation.mapping.entry.toDomainModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BackupViewModel @Inject constructor(
    private val backupOperationInteractor: BackupOperationInteractor,
    private val sessionPolicy: BackupSessionPolicy,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private data class PageEffect(val owner: Any, val effect: BackupEffect)

    private val pageEffects = Channel<PageEffect>(Channel.BUFFERED)
    val effects = pageEffects.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.effect }

    private var activePageOwner: Any? = null

    fun onAction(action: BackupUiAction) {
        when (action) {
            is BackupUiAction.CheckDirectoryPermission -> checkDirectoryPermission(action.uri)
            BackupUiAction.OpenExportOptions -> mutate(BackupMutation.ExportOptionsOpened)
            BackupUiAction.RequestImportDocument -> emitEffect(BackupEffect.SelectImportDocument)
            is BackupUiAction.SelectExportFormat -> prepareExport(action.format)
            is BackupUiAction.StartExport -> startManualExport(
                uri = action.uri,
                deleteOnFailure = action.deleteOnFailure,
            )
            is BackupUiAction.StartImport -> prepareImport(action.uri)
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
                BackupMutation.IncludedEntryTypesUpdated(action.types.toDomainModels()),
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
        if (!requireSession(sessionPolicy.regularExportDenial(), BackupOperation.EXPORT)) return
        replaceWorkflow(
            BackupMutation.ExportPrepared(
                format = format,
                fileName = backupOperationInteractor.buildExportFileName(format),
            ),
        )
    }

    private fun prepareImport(uri: Uri) {
        if (!requireSession(sessionPolicy.importDenial(), BackupOperation.IMPORT)) return
        replaceWorkflow(BackupMutation.ImportPrepared(uri))
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

    private fun startManualExport(uri: Uri, deleteOnFailure: Boolean) {
        val workflow = _uiState.value.workflow as? BackupWorkflow.AwaitingExportTarget ?: return
        val options = workflow.options
        if (!options.canSubmit || !canExecutePending(BackupOperation.EXPORT)) return
        execute(
            operation = BackupOperation.EXPORT,
            password = options.password,
        ) {
            backupOperationInteractor.exportToUri(
                request = options.toRequest(),
                targetUri = uri.toString(),
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
        val owner = activePageOwner ?: run {
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
                mutate(owner, BackupMutation.OperationSucceeded)
            }
            BackupExecutionResult.ExportSucceeded -> {
                emitEffect(owner, BackupNoticeEffect.ExportSucceeded)
                mutate(owner, BackupMutation.OperationSucceeded)
            }
            is BackupExecutionResult.ImportSucceeded -> {
                emitEffect(owner, BackupNoticeEffect.ImportSucceeded(result.result))
                mutate(owner, BackupMutation.OperationSucceeded)
            }
            BackupExecutionResult.Cancelled -> mutate(owner, BackupMutation.WorkflowClosed)
            is BackupExecutionResult.Failure -> {
                emitEffect(owner, BackupNoticeEffect.Failed(operation))
                mutate(owner, BackupMutation.OperationFailed(operation, result.error))
            }
        }
    }

    private fun canExecutePending(operation: BackupOperation): Boolean {
        if (requireSession(sessionPolicy.pendingOperationDenial(), operation)) return true
        return false
    }

    private fun requireSession(
        denial: BackupSessionDenial?,
        operation: BackupOperation,
    ): Boolean {
        if (denial == null) return true
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
        activePageOwner?.let { emitEffect(it, effect) }
    }

    private fun emitEffect(owner: Any, effect: BackupEffect) {
        if (activePageOwner === owner) {
            pageEffects.trySend(PageEffect(owner, effect))
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner !== owner) return
        activePageOwner = null
        closeWorkflow()
    }

    private fun mutate(mutation: BackupMutation) {
        _uiState.update { state -> BackupReducer.reduce(state, mutation) }
    }

    private fun mutate(owner: Any, mutation: BackupMutation) {
        if (activePageOwner === owner) mutate(mutation)
    }

    override fun onCleared() {
        _uiState.value.workflow.password().wipe()
    }
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
    sourceUri = uri.toString(),
    password = password,
)
