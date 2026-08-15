package com.aozijx.passly.data.repository.database

import android.content.Context
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.recovery.DatabaseRecoveryStore
import com.aozijx.passly.data.database.port.DatabaseController
import com.aozijx.passly.data.database.port.DatabaseQuarantineResult
import com.aozijx.passly.data.repository.database.EntryDataRefreshNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责打开、探测、重试和关闭加密数据库会话。
 * 包含内部重试机制，以处理解锁状态的瞬时竞争。
 */
@Singleton
internal class DatabaseControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val recoveryStore: DatabaseRecoveryStore,
    private val dataRefreshNotifier: EntryDataRefreshNotifier,
    private val telemetry: TelemetryReporter
) : DatabaseController {

    private companion object {
        private const val TAG = "DatabaseController"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 500L
    }

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (i in 1..MAX_RETRIES) {
            lastError = probe()
            if (lastError == null) {
                if (i > 1) report(EventLevel.INFO, "database.prewarm_recovered")
                return@withContext null
            }

            if (i < MAX_RETRIES) {
                report(EventLevel.WARN, "database.prewarm_retry", lastError)
                delay(RETRY_DELAY_MS)
            }
        }
        lastError
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        databaseSession.seal()
        databaseSession.unlock()
    }

    override suspend fun quarantineAndReinitialize(): DatabaseQuarantineResult =
        withContext(Dispatchers.IO) {
            databaseSession.seal()
            val recoveryId = recoveryStore.preserveAndClearActiveVault()
            DatabaseQuarantineResult(
                recoveryId = recoveryId,
                error = databaseSession.unlock()
            )
        }

    override suspend fun clearAndReinitialize(): Throwable? = withContext(Dispatchers.IO) {
        var recoveryError: Throwable? = null
        try {
            databaseSession.seal()
            deleteDatabaseFiles()
            VaultResourcePaths.RESOURCE_DIRECTORY_NAMES.forEach { name ->
                deleteVaultFileDirectory(name)
            }
        } catch (error: Throwable) {
            recoveryError = error
            report(EventLevel.ERROR, "database.recovery_cleanup_failed", error)
        }

        val reopenError = databaseSession.unlock()
        if (reopenError == null) {
            dataRefreshNotifier.notifyRefresh()
        }
        recoveryError ?: reopenError
    }

    override suspend fun close() {
        databaseSession.closeDatabase()
    }

    private suspend fun probe(): Throwable? =
        AppResult.runSuspendCatching {
            databaseSession.query { openHelper.writableDatabase }
        }.fold(
            onSuccess = { null },
            onFailure = { it }
        )

    private fun deleteDatabaseFiles() {
        val databaseFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        val candidates = listOf(
            databaseFile,
            File(databaseFile.path + "-wal"),
            File(databaseFile.path + "-shm"),
            File(databaseFile.path + "-journal")
        )
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val remaining = candidates.filter(File::exists)
        if (remaining.isNotEmpty()) {
            throw IOException("Unable to delete database files: ${remaining.joinToString { it.name }}")
        }
    }

    private fun deleteVaultFileDirectory(name: String) {
        val filesRoot = context.filesDir.canonicalFile
        val target = File(filesRoot, name).canonicalFile
        require(target.parentFile == filesRoot) {
            "Vault cleanup target escaped files directory"
        }
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Unable to delete Vault file directory: $name")
        }
    }

    private fun report(level: EventLevel, name: String, throwable: Throwable? = null) {
        telemetry.report(level, EventCategory.DATABASE, name, throwable)
    }
}
