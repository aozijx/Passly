package com.aozijx.passly.feature.backup.contract

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue

internal interface BackupOperationGateway {
    fun buildExportFileName(format: BackupExportFormat): String

    suspend fun checkDirectoryWritable(uri: String?): BackupExecutionResult

    suspend fun exportToConfiguredDirectory(request: BackupExportOperationRequest): BackupExecutionResult

    suspend fun exportToUri(
        request: BackupExportOperationRequest,
        targetUri: String,
        deleteOnFailure: Boolean,
    ): BackupExecutionResult

    suspend fun import(request: BackupImportOperationRequest): BackupExecutionResult
}

internal data class BackupExportOperationRequest(
    val format: BackupExportFormat,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryType>,
    val password: SensitiveValue,
    val fileName: String,
)

internal data class BackupImportOperationRequest(
    val sourceUri: String,
    val password: SensitiveValue,
    val mode: ImportMode,
    val strategy: BackupImportStrategy,
)

internal sealed interface BackupExecutionResult {
    data object DirectoryWritable : BackupExecutionResult

    data object ExportSucceeded : BackupExecutionResult

    data class ImportSucceeded(
        val result: BackupImportResult,
    ) : BackupExecutionResult

    data object Cancelled : BackupExecutionResult

    data class Failure(
        val error: AppError,
    ) : BackupExecutionResult

    data class ExportCleanupFailed(
        val error: AppError,
        val cleanupFailure: Throwable,
    ) : BackupExecutionResult
}

internal enum class BackupOperation {
    EXPORT,
    IMPORT,
    DIRECTORY_CHECK,
}
