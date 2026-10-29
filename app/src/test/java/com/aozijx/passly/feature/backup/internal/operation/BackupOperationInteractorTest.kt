package com.aozijx.passly.feature.backup.internal.operation

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
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
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupOperationInteractorTest {
    @Test
    fun `operation request is independent from presentation state`() {
        val request = exportRequest()

        assertEquals(BackupExportFormat.JSON, request.format)
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
            errorReporter = NoopErrorReporter,
        )

        val result = interactor.exportToConfiguredDirectory(exportRequest())

        assertTrue(result is BackupExecutionResult.Failure)
        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.BACKUP_EXPORT),
            gate.scope,
        )
        assertTrue(storage.createdInsideAuthorization)
    }

    @Test
    fun `directory check validates the configured tree through storage support`() = runBlocking {
        val storage = RecordingStorageSupport(RecordingAuthorizationGate())
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf(null)
            },
            backupService = UnusedBackupService,
            storageSupport = storage,
            authorizationGate = RecordingAuthorizationGate(),
            errorReporter = NoopErrorReporter,
        )

        assertEquals(
            BackupExecutionResult.DirectoryWritable,
            interactor.checkDirectoryWritable("content://documents/tree/backup"),
        )
        assertEquals("content://documents/tree/backup", storage.checkedDirectoryUri)
    }

    @Test
    fun `cancelled export deletes partial document and propagates cancellation`() = runTest {
        val gate = RecordingAuthorizationGate()
        val storage = RecordingStorageSupport(gate)
        val exportStarted = CompletableDeferred<Unit>()
        val finishExport = CompletableDeferred<Unit>()
        val target = "content://documents/export"
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf(null)
            },
            backupService = AwaitingExportService(exportStarted, finishExport),
            storageSupport = storage,
            authorizationGate = gate,
            errorReporter = NoopErrorReporter,
        )

        val export = async {
            interactor.exportToUri(exportRequest(), target, deleteOnFailure = true)
        }
        exportStarted.await()
        export.cancelAndJoin()

        assertEquals(listOf(target), storage.deletedUris)
    }

    @Test
    fun `cancelled import propagates cancellation instead of returning a failure`() = runTest {
        val gate = RecordingAuthorizationGate()
        val cancellation = java.util.concurrent.CancellationException("import cancelled")
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf(null)
            },
            backupService = object : BackupArchiveService by UnusedBackupService {
                override suspend fun import(request: BackupImportRequest): AppResult<BackupImportResult> {
                    throw cancellation
                }
            },
            storageSupport = RecordingStorageSupport(gate),
            authorizationGate = gate,
            errorReporter = NoopErrorReporter,
        )

        var propagated = false
        try {
            interactor.import(
                BackupImportOperationRequest(
                    sourceUri = "content://documents/import",
                    password = EmptySensitiveValue,
                    mode = ImportMode.APPEND,
                    strategy = BackupImportStrategy.STRICT,
                ),
            )
        } catch (failure: java.util.concurrent.CancellationException) {
            propagated = failure === cancellation
        }
        assertTrue("import cancellation must propagate", propagated)
    }

    @Test
    fun `failed partial export cleanup is attached to the original cancellation`() = runTest {
        val gate = RecordingAuthorizationGate()
        val storage = RecordingStorageSupport(gate).apply { deleteResult = false }
        val cancellation = CancellationException("export cancelled")
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf(null)
            },
            backupService = object : BackupArchiveService by UnusedBackupService {
                override suspend fun export(request: BackupExportRequest): AppResult<Unit> {
                    throw cancellation
                }
            },
            storageSupport = storage,
            authorizationGate = gate,
            errorReporter = NoopErrorReporter,
        )

        var propagated = false
        try {
            interactor.exportToUri(exportRequest(), "content://documents/export", deleteOnFailure = true)
        } catch (failure: CancellationException) {
            propagated = failure === cancellation
        }

        assertTrue("original cancellation must propagate", propagated)
        assertEquals(1, cancellation.suppressed.size)
        assertTrue(cancellation.suppressed.single() is IOException)
    }

    @Test
    fun `cancellation carried by storage results is not reported as a backup failure`() = runTest {
        val gate = RecordingAuthorizationGate()
        val storage = RecordingStorageSupport(gate)
        val interactor = BackupOperationInteractor(
            settingsSource = object : BackupDirectorySettingsSource {
                override val backupDirectoryUri = flowOf("content://tree")
            },
            backupService = UnusedBackupService,
            storageSupport = storage,
            authorizationGate = gate,
            errorReporter = NoopErrorReporter,
        )

        val targetCancellation = CancellationException("target cancelled")
        storage.targetFailure = targetCancellation
        assertCancellation(targetCancellation) { interactor.exportToConfiguredDirectory(exportRequest()) }

        val directoryCancellation = CancellationException("directory check cancelled")
        storage.directoryCheckFailure = directoryCancellation
        assertCancellation(directoryCancellation) { interactor.checkDirectoryWritable("content://tree") }
    }

    private suspend fun assertCancellation(
        expected: CancellationException,
        action: suspend () -> BackupExecutionResult,
    ) {
        var propagated = false
        try {
            action()
        } catch (failure: CancellationException) {
            propagated = failure === expected
        }
        assertTrue("cancellation must propagate", propagated)
    }

    private fun exportRequest() = BackupExportOperationRequest(
        format = BackupExportFormat.JSON,
        includeIcons = true,
        includeAttachments = false,
        includeDeleted = true,
        includedEntryTypes = setOf(EntryType.LOGIN),
        password = EmptySensitiveValue,
        fileName = "backup.json",
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
        var checkedDirectoryUri: String? = null
        val deletedUris = mutableListOf<String>()
        var targetFailure: Throwable? = null
        var directoryCheckFailure: Throwable? = null
        var deleteResult = true

        override fun buildBackupFileName(extension: String, timestamp: Long): String =
            "backup.$extension"

        override suspend fun createNamedExportTarget(
            directoryTreeUri: String,
            fileName: String,
            mimeType: String,
        ): Result<String> {
            createdInsideAuthorization = gate.inside
            return Result.failure(targetFailure ?: IllegalStateException("stop after observing authorization"))
        }

        override suspend fun deleteDocument(uri: String): Boolean {
            withContext(Dispatchers.IO) {
                deletedUris += uri
            }
            return deleteResult
        }

        override suspend fun checkDirectoryWritable(directoryTreeUri: String): Result<Unit> {
            checkedDirectoryUri = directoryTreeUri
            return directoryCheckFailure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
    }

    private object UnusedBackupService : BackupArchiveService {
        override suspend fun export(request: BackupExportRequest): AppResult<Unit> =
            error("Export should not run")

        override suspend fun import(request: BackupImportRequest): AppResult<BackupImportResult> =
            error("Import should not run")

    }

    private class AwaitingExportService(
        private val exportStarted: CompletableDeferred<Unit>,
        private val finishExport: CompletableDeferred<Unit>,
    ) : BackupArchiveService by UnusedBackupService {
        override suspend fun export(request: BackupExportRequest): AppResult<Unit> {
            exportStarted.complete(Unit)
            finishExport.await()
            error("Export should remain suspended until cancelled")
        }
    }

    private val NoopErrorReporter = AppErrorReporter { _, _ -> Unit }
}
