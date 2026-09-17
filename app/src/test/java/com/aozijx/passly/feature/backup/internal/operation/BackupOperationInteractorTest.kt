package com.aozijx.passly.feature.backup.internal.operation

import android.net.Uri
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupOperationInteractorTest {
    @Test
    fun `operation request is independent from presentation state`() {
        val request = exportRequest()

        assertEquals(BackupOperation.EXPORT, request.operation)
        assertEquals(BackupExportFormat.JSON, request.exportFormat)
        assertEquals(setOf(EntryType.LOGIN), request.includedEntryTypes)
    }

    @Test
    fun `configured export resolves storage target inside authorization scope`() = runBlocking {
        val gate = RecordingAuthorizationGate()
        val storage = RecordingStorageSupport(gate)
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf("content://tree")
            },
            backupService = UnusedBackupService,
            storageSupport = storage,
            authorizationGate = gate,
        )

        val result = interactor.exportToConfiguredDirectory(exportRequest(targetUri = null))

        assertTrue(result is BackupExecutionResult.Failure)
        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.BACKUP_EXPORT),
            gate.scope,
        )
        assertTrue(storage.createdInsideAuthorization)
    }

    private fun exportRequest(targetUri: String? = "content://backup") = BackupOperationRequest(
        operation = BackupOperation.EXPORT,
        exportFormat = BackupExportFormat.JSON,
        importMode = ImportMode.APPEND,
        includeIcons = true,
        includeAttachments = false,
        includeDeleted = true,
        includedEntryTypes = setOf(EntryType.LOGIN),
        targetUri = targetUri,
        password = EmptySensitiveValue,
        pendingExportFileName = "backup.json",
        deleteTargetOnFailure = true,
    )

    private class RecordingAuthorizationGate : AuthorizationGate {
        var inside = false
        var scope: AuthorizationScope? = null

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            inside = true
            return try {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } finally {
                inside = false
            }
        }
    }

    private class RecordingStorageSupport(
        private val gate: RecordingAuthorizationGate,
    ) : BackupStorageSupport {
        var createdInsideAuthorization = false

        override fun buildBackupFileName(extension: String, timestamp: Long): String =
            "backup.$extension"

        override fun createNamedExportTarget(
            directoryTreeUri: String,
            fileName: String,
            mimeType: String,
        ): Result<BackupStorageSupport.ExportTarget> {
            createdInsideAuthorization = gate.inside
            return Result.failure(IllegalStateException("stop after observing authorization"))
        }

        override fun deleteDocument(uri: Uri): Boolean = true
    }

    private object UnusedBackupService : BackupArchiveService {
        override suspend fun export(request: BackupExportRequest): AppResult<Unit> =
            error("Export should not run")

        override suspend fun import(request: BackupImportRequest): AppResult<Unit> =
            error("Import should not run")

        override suspend fun checkDirectoryWritable(uri: String): AppResult<Unit> =
            error("Directory check should not run")
    }
}
