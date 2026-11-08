package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupFormatRegistry
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupContainerV1Spec
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupExporter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupImporter
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupRestoreResult
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.backup.internal.model.BackupCompatibilitySummary
import com.aozijx.passly.feature.backup.internal.model.BackupExportOptions
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.testing.RecordingTestLogger
import com.aozijx.passly.testing.testLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupArchiveServiceImplTest {
    @Test
    fun exportClampsRequestedOptionsByAdapterCapabilitiesAndClearsOwnedBuffers() =
        runTest {
            val resource = byteArrayOf(1, 2, 3)
            val bundle = backupBundle(resource)
            val snapshotGateway = FakeSnapshotGateway(readBundle = bundle)
            val fileStore = FakeBackupFileStore()
            val reporter = RecordingErrorReporter()
            val exporter = FakeExportAdapter()
            val logger = RecordingTestLogger()
            val importer = FakeImportAdapter()
            val password = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
            val requestedTypes = setOf(EntryType.ACCOUNT)
            val service = service(
                snapshotGateway,
                fileStore,
                reporter,
                exporter,
                setOf(importer),
                UnconfinedTestDispatcher(testScheduler),
                logger,
            )

            exporter.supportsIcons = false
            exporter.includesAttachments = false
            exporter.encoded = byteArrayOf(4, 5, 6)
            val firstEncoded = exporter.encoded
            val firstResult =
                service.export(
                    BackupExportRequest(
                        targetUri = "content://backup/one",
                        format = exporter.formatId,
                        password = password,
                        options = BackupExportOptions(true, true, false, requestedTypes),
                    ),
                )

            exporter.supportsIcons = true
            exporter.includesAttachments = true
            exporter.encoded = byteArrayOf(7, 8, 9)
            val secondEncoded = exporter.encoded
            val secondResult =
                service.export(
                    BackupExportRequest(
                        targetUri = "content://backup/two",
                        format = exporter.formatId,
                        password = password,
                        options = BackupExportOptions(false, false, true, requestedTypes),
                    ),
                )

            assertTrue(firstResult is AppResult.Success)
            assertTrue(secondResult is AppResult.Success)
            assertEquals(
                listOf(Events.Backup.ExportStarted, Events.Backup.ExportCompleted, Events.Backup.ExportStarted, Events.Backup.ExportCompleted),
                logger.events.map { it.event },
            )
            assertEquals(
                listOf(
                    BackupSnapshotReadOptions(false, false, false, requestedTypes),
                    BackupSnapshotReadOptions(false, false, true, requestedTypes),
                ),
                snapshotGateway.readOptions,
            )
            assertEquals(listOf("content://backup/one", "content://backup/two"), fileStore.writtenUris)
            assertEquals(listOf(listOf<Byte>(4, 5, 6), listOf<Byte>(7, 8, 9)), fileStore.writtenCopies.map { it.toList() })
            assertEquals(2, exporter.passwords.size)
            assertTrue(exporter.passwords.all { it === password })
            assertTrue(firstEncoded.all { it == 0.toByte() })
            assertTrue(secondEncoded.all { it == 0.toByte() })
            assertTrue(resource.all { it == 0.toByte() })
            assertTrue(reporter.reports.isEmpty())
        }

    @Test
    fun exportFailureClearsBuffersAndReportsTheExportOperation() =
        runTest {
            val resource = byteArrayOf(1, 2, 3)
            val bundle = backupBundle(resource)
            val snapshotGateway = FakeSnapshotGateway(readBundle = bundle)
            val fileStore = FakeBackupFileStore(writeFailure = IllegalStateException("write failed"))
            val reporter = RecordingErrorReporter()
            val exporter = FakeExportAdapter(encoded = byteArrayOf(4, 5, 6))
            val encoded = exporter.encoded
            val service = service(snapshotGateway, fileStore, reporter, exporter, setOf(FakeImportAdapter()), UnconfinedTestDispatcher(testScheduler))

            val result =
                service.export(
                    BackupExportRequest(
                        targetUri = "content://backup/export",
                        format = exporter.formatId,
                    ),
                )

            assertTrue(result is AppResult.Failure)
            assertTrue(resource.all { it == 0.toByte() })
            assertEquals(1, reporter.reports.size)
            assertEquals("backup_export", reporter.reports.single().second.operation.value)
        }

    @Test
    fun exportCancellationPropagatesAndClearsBuffersWithoutReporting() =
        runTest {
            val resource = byteArrayOf(1, 2, 3)
            val bundle = backupBundle(resource)
            val cancellation = CancellationException("cancel export")
            val fileStore = FakeBackupFileStore(writeFailure = cancellation)
            val reporter = RecordingErrorReporter()
            val exporter = FakeExportAdapter(encoded = byteArrayOf(4, 5, 6))
            val encoded = exporter.encoded
            val service =
                service(
                    FakeSnapshotGateway(readBundle = bundle),
                    fileStore,
                    reporter,
                    exporter,
                    setOf(FakeImportAdapter()),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val propagated =
                try {
                    service.export(
                        BackupExportRequest(
                            targetUri = "content://backup/export",
                            format = exporter.formatId,
                        ),
                    )
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertSame(cancellation, propagated)
            assertTrue(resource.all { it == 0.toByte() })
            assertTrue(reporter.reports.isEmpty())
        }

    @Test
    fun requiredPasswordsAreCheckedBeforeSnapshotReadOrDecode() =
        runTest {
            val exportSnapshot = FakeSnapshotGateway()
            val exportReporter = RecordingErrorReporter()
            val requiredExporter = FakeExportAdapter(requiresPassword = true)
            val exportService =
                service(
                    exportSnapshot,
                    FakeBackupFileStore(),
                    exportReporter,
                    requiredExporter,
                    setOf(FakeImportAdapter()),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val exportResult =
                exportService.export(
                    BackupExportRequest(
                        targetUri = "content://backup/export",
                        format = requiredExporter.formatId,
                    ),
                )

            val importPayload = byteArrayOf(0x41, 0x42)
            val importSnapshot = FakeSnapshotGateway()
            val importReporter = RecordingErrorReporter()
            val requiredImporter = FakeImportAdapter()
            val importService =
                service(
                    importSnapshot,
                    FakeBackupFileStore(readBytes = importPayload),
                    importReporter,
                    FakeExportAdapter(),
                    setOf(requiredImporter),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val importResult =
                importService.import(
                    BackupImportRequest(
                        "content://backup/import",
                        ImportMode.APPEND,
                        format = requiredImporter.formatId,
                    ),
                )

            assertTrue(exportResult is AppResult.Failure)
            assertTrue(exportSnapshot.readOptions.isEmpty())
            assertTrue(requiredExporter.passwords.isEmpty())
            assertEquals("backup_export", exportReporter.reports.single().second.operation.value)
            assertTrue(importResult is AppResult.Failure)
            assertTrue(importSnapshot.restoredBundle == null)
            assertTrue(requiredImporter.decodedPayload == null)
            assertEquals("backup_import", importReporter.reports.single().second.operation.value)
        }

    @Test
    fun importRoutesRequestAndMergesCompatibilitySummaryWhileClearingBuffers() =
        runTest {
            val payload = byteArrayOf(0x41, 0x42)
            val resource = byteArrayOf(9, 8, 7)
            val bundle =
                backupBundle(resource).copy(
                    compatibilitySummary =
                        BackupCompatibilitySummary(
                            skippedEntryCount = 2,
                            ignoredFieldCount = 3,
                            prunedLinkCount = 4,
                            prunedResourceCount = 5,
                        ),
                )
            val snapshotGateway =
                FakeSnapshotGateway(
                    restoreResult = BackupRestoreResult(importedEntryCount = 11, existingEntryCount = 7),
                )
            val fileStore = FakeBackupFileStore(readBytes = payload)
            val reporter = RecordingErrorReporter()
            val exporter = FakeExportAdapter()
            val importer = FakeImportAdapter(bundle = bundle)
            val otherImporter = FakeImportAdapter(formatId = BackupFormats.BITWARDEN_JSON)
            val password = charArrayOf('p', 'a', 's', 's')
            val service =
                service(
                    snapshotGateway,
                    fileStore,
                    reporter,
                    exporter,
                    setOf(importer, otherImporter),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val result =
                service.import(
                    BackupImportRequest(
                        sourceUri = "content://backup/import",
                        mode = ImportMode.APPEND,
                        format = importer.formatId,
                        password = password,
                        strategy = BackupImportStrategy.STRICT,
                    ),
                )

            assertTrue(result is AppResult.Success)
            assertEquals(
                BackupImportResult(11, 7, 2, 3, 4, 5),
                (result as AppResult.Success).data,
            )
            assertEquals("content://backup/import", fileStore.readUri)
            assertEquals(payload.toList(), importer.decodedPayload?.toList())
            assertSame(password, importer.decodedPassword)
            assertEquals(BackupImportStrategy.STRICT, importer.decodedStrategy)
            assertSame(bundle, snapshotGateway.restoredBundle)
            assertEquals(ImportMode.APPEND, snapshotGateway.restoredMode)
            assertTrue(resource.all { it == 0.toByte() })
            assertTrue(reporter.reports.isEmpty())
        }

    @Test
    fun importDecodeFailureClosesInputAndReportsTheImportOperation() =
        runTest {
            val payload = byteArrayOf(0x41, 0x42)
            val fileStore = FakeBackupFileStore(readBytes = payload)
            val reporter = RecordingErrorReporter()
            val importer = FakeImportAdapter(decodeFailure = IllegalArgumentException("invalid document"))
            val service =
                service(
                    FakeSnapshotGateway(),
                    fileStore,
                    reporter,
                    FakeExportAdapter(),
                    setOf(importer),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val result =
                service.import(
                    BackupImportRequest(
                        "content://backup/import",
                        ImportMode.OVERWRITE,
                        format = importer.formatId,
                        password = charArrayOf('p'),
                    ),
                )

            assertTrue(result is AppResult.Failure)
            assertEquals(1, reporter.reports.size)
            assertEquals("backup_import", reporter.reports.single().second.operation.value)
        }

    @Test
    fun importDoesNotStartRestoreWhenEncryptedTerminalIsMissing() =
        runTest {
            val password = "restore-gate".toCharArray()
            var completeBackup = ByteArray(0)
            var missingTerminal = ByteArray(0)
            try {
                val deriveKey: (CharArray, ByteArray, KeyDerivation.Argon2idParameters) -> ByteArray = { secret, salt, parameters ->
                    ByteArray(parameters.keyLengthBits / Byte.SIZE_BITS) { index ->
                        (secret[index % secret.size].code + salt[index % salt.size]).toByte()
                    }
                }
                val encoded = ByteArrayOutputStream()
                val exportBundle =
                    BackupBundle(
                        document =
                            BackupDocument(
                                format = BackupDocument.FORMAT,
                                version = BackupDocument.CURRENT_VERSION,
                                exportedAt = 1,
                                entries = emptyList(),
                            ),
                        resourceData = emptyMap(),
                    )
                EncryptedBackupExporter().export(exportBundle, password, encoded, deriveKey)
                completeBackup = encoded.toByteArray()
                val terminalRecordSize =
                    BackupContainerV1Spec.RECORD_HEADER_LENGTH +
                        BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH +
                        BackupContainerV1Spec.TAG_LENGTH
                missingTerminal = completeBackup.copyOf(completeBackup.size - terminalRecordSize)
                val importer =
                    object : BackupImportAdapter {
                        override val formatId = BackupFormats.PASSLY_ENCRYPTED
                        override val requiresPassword = true

                        override fun decode(
                            input: InputStream,
                            password: CharArray?,
                            strategy: BackupImportStrategy,
                        ) = EncryptedBackupImporter().import(input, requireNotNull(password), strategy, deriveKey)
                    }
                val snapshotGateway = FakeSnapshotGateway()
                val reporter = RecordingErrorReporter()
                val service =
                    service(
                        snapshotGateway,
                        FakeBackupFileStore(readBytes = missingTerminal),
                        reporter,
                        FakeExportAdapter(),
                        setOf(importer),
                        UnconfinedTestDispatcher(testScheduler),
                    )

                val result =
                    service.import(
                        BackupImportRequest(
                            sourceUri = "content://backup/missing-terminal",
                            mode = ImportMode.OVERWRITE,
                            format = BackupFormats.PASSLY_ENCRYPTED,
                            password = password,
                        ),
                    )

                assertTrue(result is AppResult.Failure)
                assertEquals(null, snapshotGateway.restoredBundle)
                val failure = (result as AppResult.Failure).error
                assertTrue(failure is BackupFailed)
                assertEquals("EOFException", failure.throwableType)
                assertEquals(1, reporter.reports.size)
                assertEquals("backup_import", reporter.reports.single().second.operation.value)
            } finally {
                password.fill('\u0000')
                completeBackup.fill(0)
                missingTerminal.fill(0)
            }
        }

    @Test
    fun importRestoreFailureClosesInputAndClearsBundleResources() =
        runTest {
            val payload = byteArrayOf(0x41, 0x42)
            val resource = byteArrayOf(9, 8, 7)
            val bundle = backupBundle(resource)
            val fileStore = FakeBackupFileStore(readBytes = payload)
            val reporter = RecordingErrorReporter()
            val importer = FakeImportAdapter(bundle = bundle)
            val service =
                service(
                    FakeSnapshotGateway(restoreFailure = IllegalStateException("restore failed")),
                    fileStore,
                    reporter,
                    FakeExportAdapter(),
                    setOf(importer),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val result =
                service.import(
                    BackupImportRequest(
                        "content://backup/import",
                        ImportMode.OVERWRITE,
                        format = importer.formatId,
                        password = charArrayOf('p'),
                    ),
                )

            assertTrue(result is AppResult.Failure)
            assertTrue(resource.all { it == 0.toByte() })
            assertEquals("backup_import", reporter.reports.single().second.operation.value)
        }

    @Test
    fun importCancellationPropagatesAndClearsBundleResourcesWithoutReporting() =
        runTest {
            val payload = byteArrayOf(0x41, 0x42)
            val resource = byteArrayOf(9, 8, 7)
            val bundle = backupBundle(resource)
            val fileStore = FakeBackupFileStore(readBytes = payload)
            val reporter = RecordingErrorReporter()
            val importer = FakeImportAdapter(bundle = bundle)
            val cancellation = CancellationException("cancel restore")
            val service =
                service(
                    FakeSnapshotGateway(restoreFailure = cancellation),
                    fileStore,
                    reporter,
                    FakeExportAdapter(),
                    setOf(importer),
                    UnconfinedTestDispatcher(testScheduler),
                )

            val propagated =
                try {
                    service.import(
                        BackupImportRequest(
                            "content://backup/import",
                            ImportMode.OVERWRITE,
                            format = importer.formatId,
                            password = charArrayOf('p'),
                        ),
                    )
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertSame(cancellation, propagated)
            assertTrue(resource.all { it == 0.toByte() })
            assertTrue(reporter.reports.isEmpty())
        }

    private fun service(
        snapshotGateway: FakeSnapshotGateway,
        fileStore: FakeBackupFileStore,
        reporter: RecordingErrorReporter,
        exporter: FakeExportAdapter,
        importers: Set<BackupImportAdapter>,
        dispatcher: CoroutineDispatcher,
        logger: com.aozijx.passly.core.observability.Logger = testLogger(),
    ) =
        BackupArchiveServiceImpl(
            snapshotGateway = snapshotGateway,
            formatRegistry =
                BackupFormatRegistry(
                    setOf(exporter),
                    importers,
                    com.aozijx.passly.feature.backup.internal.archive.format.json.JsonFormatDispatcher(
                        com.aozijx.passly.feature.backup.internal.archive.format.json.JsonBackupImporter(),
                        com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter(
                            com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonParser(),
                        ),
                    ),
                ),
            fileStore = fileStore,
            errorReporter = reporter,
            logger = logger,
            ioDispatcher = dispatcher,
        )

    private class FakeSnapshotGateway(
        var readBundle: BackupBundle = backupBundle(byteArrayOf()),
        var restoreResult: BackupRestoreResult = BackupRestoreResult(0, 0),
        var readFailure: Throwable? = null,
        var restoreFailure: Throwable? = null,
    ) : BackupSnapshotGateway {
        val readOptions = mutableListOf<BackupSnapshotReadOptions>()
        var restoredBundle: BackupBundle? = null
        var restoredMode: ImportMode? = null

        override suspend fun read(options: BackupSnapshotReadOptions): BackupBundle {
            readOptions += options
            readFailure?.let { throw it }
            return readBundle
        }

        override suspend fun restore(bundle: BackupBundle, mode: ImportMode): BackupRestoreResult {
            restoredBundle = bundle
            restoredMode = mode
            restoreFailure?.let { throw it }
            return restoreResult
        }
    }

    private class FakeBackupFileStore(
        var readBytes: ByteArray = byteArrayOf(0x41, 0x42),
        var writeFailure: Throwable? = null,
    ) : BackupFileStore {
        var readUri: String? = null
        val writtenUris = mutableListOf<String>()
        val writtenCopies = mutableListOf<ByteArray>()

        override fun openInput(uri: String, maxBytes: Long): InputStream {
            readUri = uri
            return ByteArrayInputStream(readBytes.copyOf())
        }

        override fun openOutput(uri: String, maxBytes: Long): OutputStream {
            writeFailure?.let { throw it }
            writtenUris += uri
            return object : ByteArrayOutputStream() {
                override fun close() {
                    writtenCopies += toByteArray()
                    super.close()
                }
            }
        }
    }

    private class FakeExportAdapter(
        override val formatId: BackupFormatId = BackupFormats.PASSLY_JSON,
        var encoded: ByteArray = byteArrayOf(4, 5, 6),
        override val requiresPassword: Boolean = false,
    ) : BackupExportAdapter {
        override var includesAttachments = true
        override var supportsIcons = true
        val passwords = mutableListOf<CharArray?>()

        override fun encode(bundle: BackupBundle, password: CharArray?, output: OutputStream) {
            passwords += password
            try {
                output.write(encoded)
            } finally {
                encoded.fill(0)
            }
        }
    }

    private class FakeImportAdapter(
        override val formatId: BackupFormatId = BackupFormats.PASSLY_JSON,
        var bundle: BackupBundle = backupBundle(byteArrayOf()),
        var decodeFailure: Throwable? = null,
    ) : BackupImportAdapter {
        override val requiresPassword: Boolean = true
        var decodedPayload: ByteArray? = null
        var decodedPassword: CharArray? = null
        var decodedStrategy: BackupImportStrategy? = null

        override fun decode(input: InputStream, password: CharArray?, strategy: BackupImportStrategy): BackupBundle {
            val payload = input.readBytes()
            decodedPayload = payload
            decodedPassword = password
            decodedStrategy = strategy
            decodeFailure?.let { throw it }
            return bundle
        }
    }

    private class RecordingErrorReporter : AppErrorReporter {
        val reports = mutableListOf<Pair<com.aozijx.passly.core.error.model.AppError, ErrorReportContext>>()

        override suspend fun report(
            error: com.aozijx.passly.core.error.model.AppError,
            context: ErrorReportContext,
        ) {
            reports += error to context
        }
    }

}

private fun backupBundle(resource: ByteArray) =
    BackupBundle(
        document =
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1,
                entries = emptyList(),
            ),
        resourceData = mapOf("resource" to resource),
    )
