package com.aozijx.passly.feature.backup.presentation

import android.net.Uri
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal data class BackupUiState(
    val workflow: BackupWorkflow = BackupWorkflow.Idle,
)

internal sealed interface BackupWorkflow {
    data object Idle : BackupWorkflow
    data object FormatPicker : BackupWorkflow
    data class ConfigureExport(val options: BackupExportOptions) : BackupWorkflow
    data class AwaitingExportTarget(val options: BackupExportOptions) : BackupWorkflow
    data class ConfigureImport(val options: BackupImportOptions) : BackupWorkflow
    data class Executing(val operation: BackupOperation) : BackupWorkflow
    data class Failed(
        val operation: BackupOperation,
        val error: AppError,
    ) : BackupWorkflow
}

internal data class BackupExportOptions(
    val format: BackupExportFormat,
    val fileName: String,
    val password: SensitiveValue = EmptySensitiveValue,
    val includeIcons: Boolean = format.supportsResources,
    val includeAttachments: Boolean = format.supportsResources,
    val includeDeleted: Boolean = true,
    val includedEntryTypes: Set<EntryType> = BackupArchiveKeyRegistry.archiveEntryTypes,
) {
    val canSubmit: Boolean
        get() = includedEntryTypes.isNotEmpty() &&
            (!format.requiresPassword || !password.isEmpty)
}

internal data class BackupImportOptions(
    val uri: Uri,
    val password: SensitiveValue = EmptySensitiveValue,
    val mode: ImportMode = ImportMode.APPEND,
    val strategy: BackupImportStrategy = BackupImportStrategy.COMPATIBLE,
)

internal fun BackupWorkflow.password(): SensitiveValue = when (this) {
    is BackupWorkflow.ConfigureExport -> options.password
    is BackupWorkflow.AwaitingExportTarget -> options.password
    is BackupWorkflow.ConfigureImport -> options.password
    BackupWorkflow.Idle,
    BackupWorkflow.FormatPicker,
    is BackupWorkflow.Executing,
    is BackupWorkflow.Failed -> EmptySensitiveValue
}
