package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatement
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.LogStatements.Backup.Stage
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.internal.archive.di.BackupIoDispatcher
import com.aozijx.passly.feature.backup.internal.archive.format.BackupFormatRegistry
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 备份服务编排器。
 *
 * 只负责流程编排，不直接处理序列化、加密、数据库读取/写入。
 * - 导出：Snapshot Gateway → Format Exporter → FileStore
 * - 导入：FileStore → Format Importer → Snapshot Gateway
 */
@Singleton
internal class BackupArchiveServiceImpl
    @Inject
    constructor(
        private val snapshotGateway: BackupSnapshotGateway,
        private val formatRegistry: BackupFormatRegistry,
        private val fileStore: BackupFileStore,
        private val errorReporter: AppErrorReporter,
        private val logger: Logger,
        @param:BackupIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupArchiveService {
        override suspend fun export(request: BackupExportRequest): AppResult<Unit> =
            withContext(ioDispatcher) {
                runLoggedOperation(
                    operation = Operations.BackupExport,
                    started = LogStatements.Backup.exportStarted(),
                    completed = LogStatements.Backup::exportCompleted,
                ) { exportArchive(request) }
            }

        override suspend fun import(request: BackupImportRequest): AppResult<BackupImportResult> =
            withContext(ioDispatcher) {
                runLoggedOperation(
                    operation = Operations.BackupImport,
                    started = LogStatements.Backup.importStarted(),
                    completed = LogStatements.Backup::importCompleted,
                ) { importArchive(request) }
            }

        private suspend fun exportArchive(request: BackupExportRequest) {
            val adapter = formatRegistry.exporter(request.format)
            validatePassword(adapter.requiresPassword, request.password)
            val bundle =
                snapshotGateway.read(
                    BackupSnapshotReadOptions(
                        includeIcons = request.options.includeIcons && adapter.supportsIcons,
                        includeAttachments = request.options.includeAttachments && adapter.includesAttachments,
                        includeDeleted = request.options.includeDeleted,
                        includedEntryTypes = request.options.includedEntryTypes,
                    ),
                )
            try {
                logBestEffort(
                    LogStatements.Backup.phaseWithCounts(
                        Stage.EXPORT_SNAPSHOT_READY,
                        bundle.document.entries.size,
                        bundle.document.resources.size,
                    ),
                )
                logBestEffort(LogStatements.Backup.phase(Stage.EXPORT_WRITE_STARTED))
                fileStore.openOutput(request.targetUri).use { output ->
                    adapter.encode(bundle, request.password, output)
                }
                logBestEffort(LogStatements.Backup.phase(Stage.EXPORT_WRITE_COMPLETED))
            } finally {
                bundle.clearResourceData()
            }
        }

        private suspend fun importArchive(request: BackupImportRequest): BackupImportResult {
            var ownedBundle: BackupBundle? = null
            try {
                val bundle = fileStore.openInput(request.sourceUri).use { source ->
                    val selection = formatRegistry.selectImporter(request.format, source)
                    logBestEffort(LogStatements.Backup.phase(Stage.IMPORT_FORMAT_SELECTED))
                    validatePassword(selection.adapter.requiresPassword, request.password)
                    selection.adapter.decode(selection.input, request.password, request.strategy).also { ownedBundle = it }
                }
                logBestEffort(
                    LogStatements.Backup.phaseWithCounts(
                        Stage.IMPORT_DECODE_COMPLETED,
                        bundle.document.entries.size,
                        bundle.document.resources.size,
                    ),
                )
                logBestEffort(LogStatements.Backup.phase(Stage.IMPORT_RESTORE_STARTED))
                val restore = snapshotGateway.restore(bundle, request.mode)
                logBestEffort(LogStatements.Backup.phase(Stage.IMPORT_RESTORE_COMMITTED))
                return BackupImportResult(
                    importedEntryCount = restore.importedEntryCount,
                    existingEntryCount = restore.existingEntryCount,
                    skippedEntryCount = bundle.compatibilitySummary.skippedEntryCount,
                    ignoredFieldCount = bundle.compatibilitySummary.ignoredFieldCount,
                    prunedLinkCount = bundle.compatibilitySummary.prunedLinkCount,
                    prunedResourceCount = bundle.compatibilitySummary.prunedResourceCount,
                )
            } catch (error: BackupIntegrityException) {
                throw BackupFailed(reasonCode = error.reasonCode.value)
            } finally {
                ownedBundle?.clearResourceData()
            }
        }

        private fun validatePassword(
            required: Boolean,
            password: CharArray?,
        ) {
            if (required && (password == null || password.isEmpty())) {
                throw BackupFailed(reasonCode = Reasons.Backup.Authentication.BackupPasswordRequired.value)
            }
        }

        private suspend fun <T> runLoggedOperation(
            operation: OperationCode,
            started: LogStatement,
            completed: (Long) -> LogStatement,
            action: suspend () -> T,
        ): AppResult<T> =
            logger.inSpan(operation) {
                logBestEffort(started)
                val startedAt = monotonicTimeMs()
                val result = AppResult.runSuspendCatching(action)
                when (result) {
                    is AppResult.Success -> {
                        logBestEffort(completed(monotonicTimeMs() - startedAt))
                    }

                    is AppResult.Failure -> {
                        (result.error as? BackupFailed)?.let {
                            logBestEffort(
                                LogStatements.Backup.failureReason(
                                    it.reasonCode?.let(::ReasonCode) ?: Reasons.Application.Fallback.UnknownFailure,
                                ),
                            )
                        }
                        errorReporter.report(
                            error = result.error,
                            context = ErrorReportContext(operation = operation, category = LogCategory.BACKUP),
                        )
                    }
                }
                result
            }

        private fun logBestEffort(statement: LogStatement) {
            try {
                logger.log(statement)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (fatal: Error) {
                throw fatal
            } catch (_: Exception) {
                // Telemetry cannot turn a committed restore into an apparent import failure.
            }
        }
    }

private fun BackupBundle.clearResourceData() {
    resourceData.values.forEach { it.fill(0) }
}
