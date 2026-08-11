package com.aozijx.passly.feature.backup.internal.presentation

import android.net.Uri
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.domain.backup.model.BackupExportUiFormat
import com.aozijx.passly.domain.backup.model.BackupOperationStatus
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState

internal sealed interface BackupMutation {
    data object OperationStarted : BackupMutation

    data class ExportPrepared(
        val format: BackupExportUiFormat,
        val fileName: String,
    ) : BackupMutation

    data class ExportTargetSelected(
        val uri: Uri,
        val deleteOnFailure: Boolean,
    ) : BackupMutation

    data class ImportPrepared(val uri: Uri) : BackupMutation
    data class PasswordUpdated(val password: String) : BackupMutation
    data class ImportModeUpdated(val mode: ImportMode) : BackupMutation
    data class IncludeIconsUpdated(val include: Boolean) : BackupMutation
    data class IncludeAttachmentsUpdated(val include: Boolean) : BackupMutation
    data class IncludeDeletedUpdated(val include: Boolean) : BackupMutation
    data class IncludedEntryTypesUpdated(val types: Set<EntryType>) : BackupMutation
    data object OperationSucceeded : BackupMutation
    data class OperationFailed(val error: AppError) : BackupMutation
    data object PendingOperationCleared : BackupMutation
    data object PendingFieldsCleared : BackupMutation
}

internal object BackupReducer {
    fun reduce(state: BackupUiState, mutation: BackupMutation): BackupUiState =
        when (mutation) {
            BackupMutation.OperationStarted -> state.copy(
                status = BackupOperationStatus.Loading,
                error = null,
            )

            is BackupMutation.ExportPrepared -> {
                val includeResources = mutation.format.supportsResources
                state.copy(
                    isExporting = true,
                    selectedExportFormat = mutation.format,
                    backupUri = null,
                    backupPassword = "",
                    includeIcons = includeResources,
                    includeAttachments = includeResources,
                    includeDeleted = true,
                    includedEntryTypes = EntryType.entries.toSet(),
                    pendingExportFileName = mutation.fileName,
                    deleteTargetOnFailure = false,
                    status = BackupOperationStatus.Idle,
                    error = null,
                )
            }

            is BackupMutation.ExportTargetSelected -> state.copy(
                isExporting = true,
                backupUri = mutation.uri,
                deleteTargetOnFailure = mutation.deleteOnFailure,
            )

            is BackupMutation.ImportPrepared -> state.copy(
                isExporting = false,
                backupUri = mutation.uri,
                backupPassword = "",
                importMode = ImportMode.APPEND,
                pendingExportFileName = null,
                deleteTargetOnFailure = false,
                status = BackupOperationStatus.Idle,
                error = null,
            )

            is BackupMutation.PasswordUpdated -> state.copy(backupPassword = mutation.password)
            is BackupMutation.ImportModeUpdated -> state.copy(importMode = mutation.mode)
            is BackupMutation.IncludeIconsUpdated -> state.copy(includeIcons = mutation.include)
            is BackupMutation.IncludeAttachmentsUpdated ->
                state.copy(includeAttachments = mutation.include)

            is BackupMutation.IncludeDeletedUpdated -> state.copy(includeDeleted = mutation.include)
            is BackupMutation.IncludedEntryTypesUpdated ->
                state.copy(includedEntryTypes = mutation.types)

            BackupMutation.OperationSucceeded -> state.copy(
                status = BackupOperationStatus.Idle,
                error = null,
            )

            is BackupMutation.OperationFailed -> state.copy(
                status = BackupOperationStatus.Failure,
                error = mutation.error,
            )

            BackupMutation.PendingOperationCleared -> state.clearPendingFields().copy(
                status = BackupOperationStatus.Idle,
                error = null,
            )

            BackupMutation.PendingFieldsCleared -> state.clearPendingFields()
        }
}

private fun BackupUiState.clearPendingFields(): BackupUiState = copy(
    backupUri = null,
    backupPassword = "",
    pendingExportFileName = null,
    deleteTargetOnFailure = false,
)
