package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.feature.backup.contract.BackupExecutionResult
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupExportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperationGateway

internal class FakeBackupOperationGateway(
    private val performImport: suspend (BackupImportOperationRequest) -> BackupExecutionResult = {
        BackupExecutionResult.ImportSucceeded(TEST_IMPORT_RESULT)
    },
) : BackupOperationGateway {
    var exportedUri: String? = null

    override fun buildExportFileName(format: BackupExportFormat): String = "backup.${format.extension}"

    override suspend fun checkDirectoryWritable(uri: String?): BackupExecutionResult = BackupExecutionResult.DirectoryWritable

    override suspend fun exportToConfiguredDirectory(request: BackupExportOperationRequest): BackupExecutionResult =
        BackupExecutionResult.ExportSucceeded

    override suspend fun exportToUri(
        request: BackupExportOperationRequest,
        targetUri: String,
        deleteOnFailure: Boolean,
    ): BackupExecutionResult {
        exportedUri = targetUri
        return BackupExecutionResult.ExportSucceeded
    }

    override suspend fun import(request: BackupImportOperationRequest): BackupExecutionResult = performImport(request)

    private companion object {
        val TEST_IMPORT_RESULT =
            BackupImportResult(
                importedEntryCount = 1,
                existingEntryCount = 0,
                skippedEntryCount = 0,
                ignoredFieldCount = 0,
                prunedLinkCount = 0,
                prunedResourceCount = 0,
            )
    }
}
