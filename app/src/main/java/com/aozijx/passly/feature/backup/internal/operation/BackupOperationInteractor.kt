package com.aozijx.passly.feature.backup.internal.operation

import android.net.Uri
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupExportOptions
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Application use case boundary for backup flows; the ViewModel only dispatches state. */
internal class BackupOperationInteractor @Inject constructor(
    private val settingsSource: BackupDirectorySettingsSource,
    private val backupService: BackupArchiveService,
    private val storageSupport: BackupStorageSupport,
    private val authorizationGate: AuthorizationGate,
) {
    fun buildExportFileName(format: BackupExportFormat): String =
        storageSupport.buildBackupFileName(format.extension)

    suspend fun checkDirectoryWritable(uri: String?): BackupExecutionResult {
        if (uri.isNullOrBlank()) return BackupExecutionResult.Failure(BackupFailed())
        return when (val result = backupService.checkDirectoryWritable(uri)) {
            is AppResult.Success -> BackupExecutionResult.DirectoryWritable
            is AppResult.Failure -> BackupExecutionResult.Failure(result.error)
        }
    }

    suspend fun exportToConfiguredDirectory(
        request: BackupExportOperationRequest,
    ): BackupExecutionResult = authorizeOperation(AuthenticationPurpose.BACKUP_EXPORT) {
        val directoryUri = settingsSource.backupDirectoryUri.first()
            ?.takeUnless(String::isBlank)
            ?: return@authorizeOperation BackupExecutionResult.Failure(BackupFailed())
        val target = storageSupport.createNamedExportTarget(
            directoryTreeUri = directoryUri,
            fileName = request.fileName,
            mimeType = request.format.mimeType,
        ).getOrElse { error ->
            return@authorizeOperation BackupExecutionResult.Failure(AppError.fromThrowable(error))
        }
        performExport(request, target.fileUri, deleteOnFailure = true)
    }

    suspend fun exportToUri(
        request: BackupExportOperationRequest,
        targetUri: Uri,
        deleteOnFailure: Boolean,
    ): BackupExecutionResult = authorizeOperation(AuthenticationPurpose.BACKUP_EXPORT) {
        performExport(request, targetUri, deleteOnFailure)
    }

    suspend fun import(
        request: BackupImportOperationRequest,
    ): BackupExecutionResult = authorizeOperation(AuthenticationPurpose.BACKUP_IMPORT) {
        performImport(request)
    }

    private suspend fun authorizeOperation(
        purpose: AuthenticationPurpose,
        operation: suspend () -> BackupExecutionResult,
    ): BackupExecutionResult = when (
        val authorization = authorizationGate.authorize(AuthorizationScope.Global(purpose)) {
            operation()
        }
    ) {
        is AuthorizationResult.Allowed -> authorization.value
        AuthorizationResult.Cancelled -> BackupExecutionResult.Cancelled
        is AuthorizationResult.Denied -> BackupExecutionResult.Failure(BackupFailed())
    }

    private suspend fun performExport(
        request: BackupExportOperationRequest,
        targetUri: Uri,
        deleteOnFailure: Boolean,
    ): BackupExecutionResult {
        val password = request.password.takeUnless { it.isEmpty }?.toCharArray()
        return try {
            when (val result = backupService.export(
                BackupExportRequest(
                    targetUri = targetUri.toString(),
                    format = request.format.formatId,
                    password = password,
                    options = BackupExportOptions(
                        includeIcons = request.includeIcons && request.format.supportsResources,
                        includeAttachments = request.includeAttachments && request.format.supportsResources,
                        includeDeleted = request.includeDeleted,
                        includedEntryTypes = request.includedEntryTypes,
                    ),
                ),
            )) {
                is AppResult.Success -> BackupExecutionResult.ExportSucceeded
                is AppResult.Failure -> {
                    deleteFailedExport(targetUri, deleteOnFailure)
                    BackupExecutionResult.Failure(result.error)
                }
            }
        } catch (error: Exception) {
            deleteFailedExport(targetUri, deleteOnFailure)
            BackupExecutionResult.Failure(AppError.fromThrowable(error))
        } finally {
            password?.fill('\u0000')
        }
    }

    private suspend fun performImport(
        request: BackupImportOperationRequest,
    ): BackupExecutionResult {
        val password = request.password.takeUnless { it.isEmpty }?.toCharArray()
        return try {
            when (val result = backupService.import(
                BackupImportRequest(
                    sourceUri = request.sourceUri,
                    mode = request.mode,
                    strategy = request.strategy,
                    format = null,
                    password = password,
                ),
            )) {
                is AppResult.Success -> BackupExecutionResult.ImportSucceeded(result.data)
                is AppResult.Failure -> BackupExecutionResult.Failure(result.error)
            }
        } catch (error: Exception) {
            BackupExecutionResult.Failure(AppError.fromThrowable(error))
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun deleteFailedExport(targetUri: Uri, deleteOnFailure: Boolean) {
        if (deleteOnFailure) storageSupport.deleteDocument(targetUri)
    }
}

internal sealed interface BackupExecutionResult {
    data object DirectoryWritable : BackupExecutionResult
    data object ExportSucceeded : BackupExecutionResult
    data class ImportSucceeded(val result: BackupImportResult) : BackupExecutionResult
    data object Cancelled : BackupExecutionResult
    data class Failure(val error: AppError) : BackupExecutionResult
}

internal enum class BackupOperation {
    EXPORT,
    IMPORT,
    DIRECTORY_CHECK,
}
