package com.aozijx.passly.feature.backup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.contract.BackupExecutionResult
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupExportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupOperation
import com.aozijx.passly.feature.backup.contract.BackupOperationGateway
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class BackupViewModel
    @Inject
    constructor(
        private val backupOperationGateway: BackupOperationGateway,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val errorReporter: AppErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BackupUiState())
        val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

        private val pageEffects = PageEffects<BackupEffect>()
        val effects = pageEffects.effects
        private var pendingImportDocument: PendingImportDocument? = null
        private var pendingExportTarget: PendingExportTarget? = null

        fun onAction(action: BackupUiAction) {
            if (_uiState.value.workflow is BackupWorkflow.Executing) return
            when (action) {
                is BackupUiAction.CheckDirectoryPermission -> {
                    checkDirectoryPermission(action.uri)
                }

                BackupUiAction.OpenExportOptions -> {
                    mutate(BackupMutation.ExportOptionsOpened)
                }

                is BackupUiAction.RequestImportDocument -> {
                    requestImportDocument(action.pageOwner)
                }

                is BackupUiAction.SelectExportFormat -> {
                    prepareExport(action.format)
                }

                is BackupUiAction.StartExport -> {
                    completeExportTarget(action)
                }

                is BackupUiAction.StartImport -> {
                    startImport(action)
                }

                is BackupUiAction.UpdatePassword -> {
                    replacePassword(
                        OwnedChars.fromString(action.password),
                    )
                }

                is BackupUiAction.UpdateImportMode -> {
                    mutate(BackupMutation.ImportModeUpdated(action.mode))
                }

                is BackupUiAction.UpdateImportStrategy -> {
                    mutate(BackupMutation.ImportStrategyUpdated(action.strategy))
                }

                is BackupUiAction.UpdateIncludeIcons -> {
                    mutate(BackupMutation.IncludeIconsUpdated(action.include))
                }

                is BackupUiAction.UpdateIncludeAttachments -> {
                    mutate(BackupMutation.IncludeAttachmentsUpdated(action.include))
                }

                is BackupUiAction.UpdateIncludeDeleted -> {
                    mutate(BackupMutation.IncludeDeletedUpdated(action.include))
                }

                is BackupUiAction.ToggleIncludedEntryType -> {
                    val options = (_uiState.value.workflow as? BackupWorkflow.ConfigureExport)?.options ?: return
                    mutate(
                        BackupMutation.IncludedEntryTypesUpdated(
                            options.includedEntryTypes.toMutableSet().apply {
                                if (!add(action.type)) remove(action.type)
                            },
                        ),
                    )
                }

                BackupUiAction.SelectAllEntryTypes -> {
                    mutate(BackupMutation.IncludedEntryTypesUpdated(EntryType.entries.toSet()))
                }

                BackupUiAction.ClearEntryTypes -> {
                    mutate(BackupMutation.IncludedEntryTypesUpdated(emptySet()))
                }

                is BackupUiAction.SubmitExport -> {
                    submitExport(action.useConfiguredDirectory)
                }

                BackupUiAction.SubmitImport -> {
                    submitImport()
                }

                BackupUiAction.DismissOptions -> {
                    closeWorkflow()
                }
            }
        }

        private fun checkDirectoryPermission(uri: String?) {
            execute(
                operation = BackupOperation.DIRECTORY_CHECK,
                password = EmptySensitiveValue,
            ) {
                backupOperationGateway.checkDirectoryWritable(uri)
            }
        }

        private fun prepareExport(format: BackupExportFormat) {
            if (!requireSession(BackupOperation.EXPORT)) return
            replaceWorkflow(
                BackupMutation.ExportPrepared(
                    format = format,
                    fileName = backupOperationGateway.buildExportFileName(format),
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
            if (!emitEffect(owner, BackupEffect.SelectImportDocument(request.requestId))) {
                pendingImportDocument = null
            }
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
            if (!requireSession(BackupOperation.EXPORT)) return
            if (useConfiguredDirectory) {
                execute(
                    operation = BackupOperation.EXPORT,
                    password = options.password,
                    exportFileName = options.fileName,
                ) {
                    backupOperationGateway.exportToConfiguredDirectory(options.toRequest())
                }
            } else {
                val owner = pageEffects.currentOwner ?: return
                val request = PendingExportTarget(owner, UUID.randomUUID().toString())
                pendingExportTarget = request
                mutate(BackupMutation.ExportTargetRequested)
                val delivered =
                    emitEffect(
                        owner,
                        BackupEffect.SelectExportDocument(
                            format = options.format,
                            fileName = options.fileName,
                            requestId = request.requestId,
                        ),
                    )
                if (!delivered) {
                    pendingExportTarget = null
                    mutate(BackupMutation.ExportTargetRequestFailed)
                }
            }
        }

        private fun completeExportTarget(action: BackupUiAction.StartExport) {
            val pending = pendingExportTarget ?: return
            if (pending.pageOwner !== action.pageOwner || pending.requestId != action.requestId) return
            pendingExportTarget = null
            if (!pageEffects.isActive(action.pageOwner)) return
            action.uri?.let { uri ->
                startManualExport(uri, action.deleteOnFailure)
            } ?: closeWorkflow()
        }

        private fun startManualExport(
            uri: String,
            deleteOnFailure: Boolean,
        ) {
            val workflow = _uiState.value.workflow as? BackupWorkflow.AwaitingExportTarget ?: return
            val options = workflow.options
            if (!options.canSubmit || !requireSession(BackupOperation.EXPORT)) return
            execute(
                operation = BackupOperation.EXPORT,
                password = options.password,
                exportFileName = options.fileName,
            ) {
                backupOperationGateway.exportToUri(
                    request = options.toRequest(),
                    targetUri = uri,
                    deleteOnFailure = deleteOnFailure,
                )
            }
        }

        private fun submitImport() {
            val workflow = _uiState.value.workflow as? BackupWorkflow.ConfigureImport ?: return
            if (!requireSession(BackupOperation.IMPORT)) return
            val options = workflow.options
            execute(
                operation = BackupOperation.IMPORT,
                password = options.password,
            ) {
                backupOperationGateway.import(options.toRequest())
            }
        }

        private fun execute(
            operation: BackupOperation,
            password: SensitiveValue,
            exportFileName: String? = null,
            block: suspend () -> BackupExecutionResult,
        ) {
            val owner =
                pageEffects.currentOwner ?: run {
                    password.wipe()
                    return
                }
            mutate(BackupMutation.OperationStarted(operation))
            viewModelScope.launch {
                try {
                    applyResult(block(), operation, owner, exportFileName)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    val error = AppError.fromThrowable(failure)
                    errorReporter.report(
                        error,
                        ErrorReportContext(
                            operation = OperationCode("backup_viewmodel_execute"),
                            category = LogCategory.BACKUP,
                        ),
                    )
                    applyResult(BackupExecutionResult.Failure(error), operation, owner, exportFileName)
                } finally {
                    password.wipe()
                }
            }
        }

        private fun applyResult(
            result: BackupExecutionResult,
            operation: BackupOperation,
            owner: Any,
            exportFileName: String?,
        ) {
            when (result) {
                BackupExecutionResult.DirectoryWritable -> {
                    emitEffect(owner, BackupNoticeEffect.DirectoryWritable)
                    mutateIfExecuting(operation, BackupMutation.OperationSucceeded)
                }

                BackupExecutionResult.ExportSucceeded -> {
                    emitEffect(owner, BackupNoticeEffect.ExportSucceeded)
                    mutateIfExecuting(
                        operation,
                        exportFileName?.let(BackupMutation::ExportSucceeded)
                            ?: BackupMutation.OperationSucceeded,
                    )
                }

                is BackupExecutionResult.ImportSucceeded -> {
                    emitEffect(owner, BackupNoticeEffect.ImportSucceeded(result.result))
                    mutateIfExecuting(operation, BackupMutation.OperationSucceeded)
                }

                BackupExecutionResult.Cancelled -> {
                    mutateIfExecuting(operation, BackupMutation.WorkflowClosed)
                }

                is BackupExecutionResult.Failure -> {
                    val reasonCode = (result.error as? BackupFailed)?.reasonCode?.let(::ReasonCode)
                    emitEffect(owner, BackupNoticeEffect.Failed(operation, reasonCode))
                    mutateIfExecuting(operation, BackupMutation.OperationFailed(operation, result.error))
                }

                is BackupExecutionResult.ExportCleanupFailed -> {
                    emitEffect(owner, BackupNoticeEffect.ExportCleanupFailed)
                    mutateIfExecuting(operation, BackupMutation.OperationFailed(operation, result.error))
                }
            }
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

        private fun emitEffect(effect: BackupEffect): Boolean = pageEffects.tryEmit(effect)

        private fun emitEffect(
            owner: Any,
            effect: BackupEffect,
        ): Boolean = pageEffects.tryEmit(owner, effect)

        fun onPageEntered(): Any {
            val previousOwner = pageEffects.currentOwner
            val owner = pageEffects.enter()
            if (previousOwner != null) {
                pendingImportDocument = null
                pendingExportTarget = null
                if (_uiState.value.workflow !is BackupWorkflow.Executing) closeWorkflow()
            }
            return owner
        }

        fun onPageExited(owner: Any) {
            if (!pageEffects.isActive(owner)) return
            pageEffects.exit(owner)
            if (pendingImportDocument?.pageOwner === owner) pendingImportDocument = null
            if (pendingExportTarget?.pageOwner === owner) pendingExportTarget = null
            if (_uiState.value.workflow !is BackupWorkflow.Executing) closeWorkflow()
        }

        private fun mutate(mutation: BackupMutation) {
            _uiState.update { state -> BackupReducer.reduce(state, mutation) }
        }

        private fun mutateIfExecuting(
            operation: BackupOperation,
            mutation: BackupMutation,
        ) {
            if (_uiState.value.workflow == BackupWorkflow.Executing(operation)) mutate(mutation)
        }

        override fun onCleared() {
            pendingImportDocument = null
            _uiState.value.workflow
                .password()
                .wipe()
        }

        private data class PendingImportDocument(
            val pageOwner: Any,
            val requestId: String,
        )

        private data class PendingExportTarget(
            val pageOwner: Any,
            val requestId: String,
        )
    }

private fun BackupExportOptions.toRequest() =
    BackupExportOperationRequest(
        format = format,
        includeIcons = includeIcons,
        includeAttachments = includeAttachments,
        includeDeleted = includeDeleted,
        includedEntryTypes = includedEntryTypes,
        password = password,
        fileName = fileName,
    )

private fun BackupImportOptions.toRequest() =
    BackupImportOperationRequest(
        mode = mode,
        strategy = strategy,
        sourceUri = sourceUri,
        password = password,
    )
