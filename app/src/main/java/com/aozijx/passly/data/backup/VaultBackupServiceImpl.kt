package com.aozijx.passly.data.backup

import com.aozijx.passly.app.di.IoDispatcher
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.reporting.AppErrorReporter
import com.aozijx.passly.core.telemetry.reporting.ErrorReportContext
import com.aozijx.passly.data.backup.format.BackupFormatRegistry
import com.aozijx.passly.data.backup.io.BackupFileStore
import com.aozijx.passly.data.backup.source.VaultBackupReader
import com.aozijx.passly.data.backup.source.VaultBackupRestorer
import com.aozijx.passly.domain.backup.model.BackupExportRequest
import com.aozijx.passly.domain.backup.model.BackupImportRequest
import com.aozijx.passly.domain.backup.service.VaultBackupService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份服务编排器。
 *
 * 只负责流程编排，不直接处理序列化、加密、数据库读取/写入。
 * - 导出：VaultBackupReader → Format Exporter → FileStore
 * - 导入：FileStore → Format Importer → VaultBackupRestorer
 */
@Singleton
internal class VaultBackupServiceImpl @Inject constructor(
    private val backupReader: VaultBackupReader,
    private val backupRestorer: VaultBackupRestorer,
    private val formatRegistry: BackupFormatRegistry,
    private val fileStore: BackupFileStore,
    private val errorReporter: AppErrorReporter,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VaultBackupService {

    override suspend fun export(
        request: BackupExportRequest
    ): AppResult<Unit> = withContext(ioDispatcher) {
        AppResult.runSuspendCatching {
            val adapter = formatRegistry.exporter(request.format)
            validatePassword(adapter.requiresPassword, request.password)
            val bundle = backupReader.readBundle(
                includeIcons = request.options.includeIcons && adapter.supportsIcons,
                includeAttachments =
                    request.options.includeAttachments && adapter.includesAttachments,
                includeDeleted = request.options.includeDeleted,
                includedEntryTypes = request.options.includedEntryTypes
            )
            try {
                val encoded = adapter.encode(bundle, request.password)
                try {
                    fileStore.writeBytes(request.targetUri, encoded)
                } finally {
                    encoded.fill(0)
                }
            } finally {
                bundle.clearResourceData()
            }
        }.onFailure { report(it, "backup.export") }
    }

    override suspend fun import(
        request: BackupImportRequest
    ): AppResult<Unit> = withContext(ioDispatcher) {
        AppResult.runSuspendCatching {
            val payload = fileStore.readBytesSafely(request.sourceUri)
            try {
                val adapter = formatRegistry.importer(request.format, payload)
                validatePassword(adapter.requiresPassword, request.password)
                val bundle = adapter.decode(payload, request.password)
                try {
                    backupRestorer.restore(bundle, request.mode)
                } finally {
                    bundle.clearResourceData()
                }
            } finally {
                payload.fill(0)
            }
        }.onFailure { report(it, "backup.import") }
    }

    override suspend fun checkDirectoryWritable(uri: String): AppResult<Unit> =
        fileStore.checkWritable(uri).onFailure { report(it, "backup.checkWritable") }

    private fun validatePassword(required: Boolean, password: CharArray?) {
        if (required && (password == null || password.isEmpty())) throw BackupFailed()
    }

    private fun report(error: com.aozijx.passly.core.error.model.AppError, operation: String) {
        errorReporter.report(
            error = error,
            context = ErrorReportContext(
                operation = OperationCode(operation),
                category = EventCategory.BACKUP
            )
        )
    }
}

private fun com.aozijx.passly.data.backup.model.BackupBundle.clearResourceData() {
    resourceData.values.forEach { it.fill(0) }
}
