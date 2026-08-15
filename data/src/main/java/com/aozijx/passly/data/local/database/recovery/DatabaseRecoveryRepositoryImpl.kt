package com.aozijx.passly.data.local.database.recovery

import android.content.Context
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.database.model.DatabaseRecoverySelection
import com.aozijx.passly.data.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.data.database.port.DatabaseRecoveryRepository
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.repository.database.EntryDataRefreshNotifier
import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.core.crypto.FieldEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DatabaseRecoveryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: DatabaseRecoveryStore,
    private val databaseProvider: DatabaseProvider,
    private val sessionKeySource: SessionKeySource,
    private val scanner: DatabaseRecoveryScanner,
    private val importer: DatabaseRecoveryImporter,
    private val fieldEncryptor: FieldEncryptor,
    private val dataRefreshNotifier: EntryDataRefreshNotifier,
    private val telemetry: TelemetryReporter,
) : DatabaseRecoveryRepository {
    override suspend fun listPackages(): List<DatabaseRecoveryPackage> =
        withContext(Dispatchers.IO) { store.listPackages() }

    override suspend fun scan(packageId: String): DatabaseRecoveryScan =
        withRecoveryDatabase(packageId) { verified, source ->
            runCatching { scanner.scan(verified, source) }
                .onSuccess { result ->
                    store.updateStatus(
                        packageId,
                        if (result.isPartial) DatabaseRecoveryStatus.PARTIALLY_RECOVERABLE
                        else DatabaseRecoveryStatus.RECOVERABLE,
                    )
                    report(EventLevel.INFO, "database.recovery.scan_completed")
                }
                .onFailure {
                    store.updateStatus(packageId, DatabaseRecoveryStatus.UNREADABLE)
                    report(EventLevel.WARN, "database.recovery.scan_failed", it)
                }
                .getOrThrow()
        }

    override suspend fun restore(
        packageId: String,
        selection: DatabaseRecoverySelection,
    ): DatabaseRecoveryReport {
        require(selection.entryTypes.isNotEmpty()) { "At least one entry type must be selected" }
        val plan = withRecoveryDatabase(packageId) { verified, source ->
            scanner.prepare(verified, source, selection.entryTypes)
        }
        val result = runCatching { importer.restore(plan) }
            .onFailure { report(EventLevel.ERROR, "database.recovery.restore_failed", it) }
            .getOrThrow()
        runCatching {
            store.updateStatus(packageId, DatabaseRecoveryStatus.RESTORED)
            persistReport(result)
        }.onFailure { report(EventLevel.WARN, "database.recovery.report_persist_failed", it) }
        dataRefreshNotifier.notifyRefresh()
        report(EventLevel.INFO, "database.recovery.restore_completed")
        return result
    }

    override suspend fun delete(packageId: String) = withContext(Dispatchers.IO) {
        store.delete(packageId)
        report(EventLevel.INFO, "database.recovery.package_deleted")
    }

    private suspend fun <T> withRecoveryDatabase(
        packageId: String,
        block: suspend (DatabaseRecoveryStore.VerifiedPackage, AppDatabase) -> T,
    ): T = withContext(Dispatchers.IO) {
        val verified = store.verify(packageId)
        val workName = "recovery_${sha256(packageId).take(20)}.db"
        deleteWorkDatabase(workName)
        val workDatabase = context.getDatabasePath(workName)
        check(workDatabase.parentFile?.exists() == true || workDatabase.parentFile?.mkdirs() == true)
        verified.databaseDirectory.listFiles().orEmpty().forEach { source ->
            val suffix = source.name.removePrefix(DatabaseSchema.DATABASE_NAME)
            source.copyTo(File(workDatabase.path + suffix), overwrite = false)
        }
        val key = sessionKeySource.copyKey()
        val database = try {
            databaseProvider.open(workName, key)
        } catch (error: Throwable) {
            deleteWorkDatabase(workName)
            throw error
        } finally {
            key.fill(0)
        }
        try {
            block(verified, database)
        } finally {
            database.close()
            deleteWorkDatabase(workName)
        }
    }

    private fun deleteWorkDatabase(name: String) {
        context.deleteDatabase(name)
        val main = context.getDatabasePath(name)
        listOf(main, File(main.path + "-wal"), File(main.path + "-shm"), File(main.path + "-journal"))
            .filter(File::exists)
            .forEach { check(it.delete()) { "Unable to remove recovery work database" } }
    }

    private fun persistReport(report: DatabaseRecoveryReport) {
        val text = buildString {
            appendLine("version=1")
            appendLine("restoredEntries=${report.restoredEntries}")
            appendLine("skippedConflicts=${report.skippedConflicts}")
            appendLine("restoredAttachments=${report.restoredAttachments}")
            appendLine("skippedResources=${report.skippedResources}")
            appendLine("restoredRevisions=${report.restoredRevisions}")
            appendLine("restoredLinks=${report.restoredLinks}")
            report.issues.forEachIndexed { index, issue ->
                appendLine(
                    "issue.$index=${issue.category}|${issue.anonymousRecordId.orEmpty()}|" +
                        issue.reasonCode,
                )
            }
        }
        val encrypted = fieldEncryptor.encrypt(
            text,
            "database_recovery:${report.packageId}:report".toByteArray(),
        )
        try {
            store.writeEncryptedReport(report.packageId, encrypted)
        } finally {
            encrypted.fill(0)
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun report(level: EventLevel, name: String, error: Throwable? = null) {
        telemetry.report(level, EventCategory.DATABASE, name, error)
    }
}
