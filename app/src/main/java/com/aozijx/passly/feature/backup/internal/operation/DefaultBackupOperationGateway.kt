package com.aozijx.passly.feature.backup.internal.operation

import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.backup.contract.BackupExecutionResult
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupExportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperationGateway
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.archive.platform.preferBackupCleanupFailure
import com.aozijx.passly.feature.backup.internal.model.BackupExportOptions
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.model.toFormatId
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Internal implementation of the feature-owned operation boundary. */
internal class DefaultBackupOperationGateway
    @Inject
    constructor(
        private val settingsSource: BackupDirectorySettingsSource,
        private val backupService: BackupArchiveService,
        private val storageSupport: BackupStorageSupport,
        private val authorizationGate: AuthorizationGate,
        private val errorReporter: AppErrorReporter,
    ) : BackupOperationGateway {
        override fun buildExportFileName(format: BackupExportFormat): String = storageSupport.buildBackupFileName(format.extension)

        override suspend fun checkDirectoryWritable(uri: String?): BackupExecutionResult {
            if (uri.isNullOrBlank()) return BackupExecutionResult.Failure(BackupFailed())
            val checkResult =
                try {
                    storageSupport.checkDirectoryWritable(uri)
                } catch (failure: Throwable) {
                    return failureResult(failure, OperationCode("backup_check_writable"))
                }
            return checkResult.fold(
                onSuccess = { BackupExecutionResult.DirectoryWritable },
                onFailure = { failure -> failureResult(failure, OperationCode("backup_check_writable")) },
            )
        }

        override suspend fun exportToConfiguredDirectory(request: BackupExportOperationRequest): BackupExecutionResult =
            authorizeOperation(AuthenticationPurpose.BACKUP_EXPORT) {
                val directoryUri =
                    settingsSource.backupDirectoryUri
                        .first()
                        ?.takeUnless(String::isBlank)
                        ?: return@authorizeOperation BackupExecutionResult.Failure(BackupFailed())
                val targetUri =
                    storageSupport
                        .createNamedExportTarget(
                            directoryTreeUri = directoryUri,
                            fileName = request.fileName,
                            mimeType = request.format.mimeType,
                        ).getOrElse { error ->
                            return@authorizeOperation failureResult(error, OperationCode("backup_create_export_target"))
                        }
                performExport(request, targetUri, deleteOnFailure = true)
            }

        override suspend fun exportToUri(
            request: BackupExportOperationRequest,
            targetUri: String,
            deleteOnFailure: Boolean,
        ): BackupExecutionResult =
            authorizeOperation(AuthenticationPurpose.BACKUP_EXPORT) {
                performExport(request, targetUri, deleteOnFailure)
            }

        override suspend fun import(request: BackupImportOperationRequest): BackupExecutionResult =
            authorizeOperation(AuthenticationPurpose.BACKUP_IMPORT) {
                performImport(request)
            }

        private suspend fun authorizeOperation(
            purpose: AuthenticationPurpose,
            operation: suspend () -> BackupExecutionResult,
        ): BackupExecutionResult {
            val authorization =
                try {
                    authorizationGate.authorize(AuthorizationScope.Global(purpose)) {
                        operation()
                    }
                } catch (failure: Throwable) {
                    return failureResult(failure, OperationCode(purpose.name.lowercase()))
                }
            return when (authorization) {
                is AuthorizationResult.Allowed -> authorization.value
                AuthorizationResult.Cancelled -> BackupExecutionResult.Cancelled
                is AuthorizationResult.Denied -> BackupExecutionResult.Failure(BackupFailed())
            }
        }

        private suspend fun performExport(
            request: BackupExportOperationRequest,
            targetUri: String,
            deleteOnFailure: Boolean,
        ): BackupExecutionResult {
            val password = request.password.takeUnless { it.isEmpty }?.toCharArray()
            return try {
                when (
                    val result =
                        backupService.export(
                            BackupExportRequest(
                                targetUri = targetUri,
                                format = request.format.toFormatId(),
                                password = password,
                                options =
                                    BackupExportOptions(
                                        includeIcons = request.includeIcons && request.format.supportsResources,
                                        includeAttachments = request.includeAttachments && request.format.supportsResources,
                                        includeDeleted = request.includeDeleted,
                                        includedEntryTypes = request.includedEntryTypes,
                                    ),
                            ),
                        )
                ) {
                    is AppResult.Success -> {
                        BackupExecutionResult.ExportSucceeded
                    }

                    is AppResult.Failure -> {
                        if (deleteOnFailure) {
                            val cleanupFailure = tryDeleteFailedExport(targetUri)
                            if (cleanupFailure is CancellationException || cleanupFailure is Error) throw cleanupFailure
                            if (cleanupFailure != null) {
                                failureResult(cleanupFailure, OperationCode("backup_export_cleanup"))
                                return BackupExecutionResult.ExportCleanupFailed(result.error, cleanupFailure)
                            }
                        }
                        BackupExecutionResult.Failure(result.error)
                    }
                }
            } catch (cancelled: CancellationException) {
                if (deleteOnFailure) {
                    val cleanupFailure = tryDeleteFailedExport(targetUri)
                    if (cleanupFailure != null) {
                        throw preferBackupCleanupFailure(cancelled, cleanupFailure)
                    }
                }
                throw cancelled
            } catch (fatal: Error) {
                if (deleteOnFailure) {
                    val cleanupFailure = tryDeleteFailedExport(targetUri)
                    if (cleanupFailure != null) {
                        throw preferBackupCleanupFailure(fatal, cleanupFailure)
                    }
                }
                throw fatal
            } catch (error: Exception) {
                if (deleteOnFailure) {
                    val cleanupFailure = tryDeleteFailedExport(targetUri)
                    if (cleanupFailure != null) {
                        val preferredFailure = preferBackupCleanupFailure(error, cleanupFailure)
                        if (preferredFailure !== error) throw preferredFailure
                    }
                }
                failureResult(error, OperationCode("backup_export"))
            } finally {
                password?.fill('\u0000')
            }
        }

        private suspend fun performImport(request: BackupImportOperationRequest): BackupExecutionResult {
            val password = request.password.takeUnless { it.isEmpty }?.toCharArray()
            return try {
                when (
                    val result =
                        backupService.import(
                            BackupImportRequest(
                                sourceUri = request.sourceUri,
                                mode = request.mode,
                                strategy = request.strategy,
                                format = null,
                                password = password,
                            ),
                        )
                ) {
                    is AppResult.Success -> BackupExecutionResult.ImportSucceeded(result.data)
                    is AppResult.Failure -> BackupExecutionResult.Failure(result.error)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failureResult(error, OperationCode("backup_import"))
            } finally {
                password?.fill('\u0000')
            }
        }

        private suspend fun deleteFailedExport(targetUri: String): Boolean = storageSupport.deleteDocument(targetUri)

        private suspend fun tryDeleteFailedExport(targetUri: String): Throwable? =
            withContext(NonCancellable) {
                try {
                    if (deleteFailedExport(targetUri)) null else IOException("Unable to delete incomplete backup export")
                } catch (failure: Throwable) {
                    failure
                }
            }

        private suspend fun failureResult(
            failure: Throwable,
            operation: OperationCode,
        ): BackupExecutionResult.Failure {
            if (failure is CancellationException || failure !is Exception) throw failure
            val error = AppError.fromThrowable(failure)
            errorReporter.report(
                error,
                ErrorReportContext(operation = operation, category = LogCategory.BACKUP),
            )
            return BackupExecutionResult.Failure(error)
        }
    }
