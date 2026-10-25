package com.aozijx.passly.feature.backup.presentation

import android.net.Uri
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal sealed interface BackupMutation {
    data object ExportOptionsOpened : BackupMutation
    data object WorkflowClosed : BackupMutation
    data class ExportPrepared(
        val format: BackupExportFormat,
        val fileName: String,
    ) : BackupMutation
    data object ExportTargetRequested : BackupMutation
    data class ImportPrepared(val uri: Uri) : BackupMutation
    data class PasswordUpdated(val password: SensitiveValue) : BackupMutation
    data class ImportModeUpdated(val mode: ImportMode) : BackupMutation
    data class ImportStrategyUpdated(val strategy: BackupImportStrategy) : BackupMutation
    data class IncludeIconsUpdated(val include: Boolean) : BackupMutation
    data class IncludeAttachmentsUpdated(val include: Boolean) : BackupMutation
    data class IncludeDeletedUpdated(val include: Boolean) : BackupMutation
    data class IncludedEntryTypesUpdated(val types: Set<EntryType>) : BackupMutation
    data class OperationStarted(val operation: BackupOperation) : BackupMutation
    data object OperationSucceeded : BackupMutation
    data class OperationFailed(
        val operation: BackupOperation,
        val error: AppError,
    ) : BackupMutation
}

internal object BackupReducer {
    fun reduce(state: BackupUiState, mutation: BackupMutation): BackupUiState = when (mutation) {
        BackupMutation.ExportOptionsOpened -> state.copy(
            workflow = BackupWorkflow.FormatPicker,
        )
        BackupMutation.WorkflowClosed,
        BackupMutation.OperationSucceeded -> state.copy(workflow = BackupWorkflow.Idle)
        is BackupMutation.ExportPrepared -> state.copy(
            workflow = BackupWorkflow.ConfigureExport(
                BackupExportOptions(
                    format = mutation.format,
                    fileName = mutation.fileName,
                ),
            ),
        )
        BackupMutation.ExportTargetRequested -> state.updateExportOptions { options ->
            BackupWorkflow.AwaitingExportTarget(options)
        }
        is BackupMutation.ImportPrepared -> state.copy(
            workflow = BackupWorkflow.ConfigureImport(BackupImportOptions(mutation.uri)),
        )
        is BackupMutation.PasswordUpdated -> state.updateOptionsPassword(mutation.password)
        is BackupMutation.ImportModeUpdated -> state.updateImportOptions {
            copy(mode = mutation.mode)
        }
        is BackupMutation.ImportStrategyUpdated -> state.updateImportOptions {
            copy(strategy = mutation.strategy)
        }
        is BackupMutation.IncludeIconsUpdated -> state.updateExportOptions { options ->
            BackupWorkflow.ConfigureExport(options.copy(includeIcons = mutation.include))
        }
        is BackupMutation.IncludeAttachmentsUpdated -> state.updateExportOptions { options ->
            BackupWorkflow.ConfigureExport(options.copy(includeAttachments = mutation.include))
        }
        is BackupMutation.IncludeDeletedUpdated -> state.updateExportOptions { options ->
            BackupWorkflow.ConfigureExport(options.copy(includeDeleted = mutation.include))
        }
        is BackupMutation.IncludedEntryTypesUpdated -> state.updateExportOptions { options ->
            BackupWorkflow.ConfigureExport(options.copy(includedEntryTypes = mutation.types))
        }
        is BackupMutation.OperationStarted -> state.copy(
            workflow = BackupWorkflow.Executing(mutation.operation),
        )
        is BackupMutation.OperationFailed -> state.copy(
            workflow = BackupWorkflow.Failed(mutation.operation, mutation.error),
        )
    }
}

private inline fun BackupUiState.updateExportOptions(
    transform: (BackupExportOptions) -> BackupWorkflow,
): BackupUiState {
    val options = when (val current = workflow) {
        is BackupWorkflow.ConfigureExport -> current.options
        is BackupWorkflow.AwaitingExportTarget -> current.options
        else -> return this
    }
    return copy(workflow = transform(options))
}

private inline fun BackupUiState.updateImportOptions(
    transform: BackupImportOptions.() -> BackupImportOptions,
): BackupUiState {
    val current = workflow as? BackupWorkflow.ConfigureImport ?: return this
    return copy(workflow = current.copy(options = current.options.transform()))
}

private fun BackupUiState.updateOptionsPassword(password: SensitiveValue): BackupUiState =
    when (val current = workflow) {
        is BackupWorkflow.ConfigureExport -> copy(
            workflow = current.copy(options = current.options.copy(password = password)),
        )
        is BackupWorkflow.ConfigureImport -> copy(
            workflow = current.copy(options = current.options.copy(password = password)),
        )
        is BackupWorkflow.AwaitingExportTarget -> copy(
            workflow = current.copy(options = current.options.copy(password = password)),
        )
        else -> this
    }
