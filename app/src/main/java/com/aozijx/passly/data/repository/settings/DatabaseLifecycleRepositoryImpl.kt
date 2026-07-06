package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.DatabaseConfig
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DatabaseLifecycleRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionManager: DatabaseSessionManager
) : DatabaseLifecycleRepository {
    private companion object {
        private const val TAG = "DatabaseLifecycle"
        private const val MAX_RECOVERY_BACKUPS = 3
    }

    private val appContext = context.applicationContext

    @Volatile
    private var autoRecoveryNotice: String? = null

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        val firstError = warmUpOnce() ?: return@withContext null

        if (isRecoverableInitError(firstError)) {
            Logcat.w(
                TAG,
                "Detected recoverable DB init failure, attempting auto recovery",
                firstError
            )
            val recovered = attemptAutoRecovery()
            if (!recovered) {
                Logcat.e(TAG, "Auto recovery aborted: snapshot failed, original DB kept")
                return@withContext firstError
            }
            val secondError = warmUpOnce()
            if (secondError == null) {
                autoRecoveryNotice = "检测到数据库异常，已自动重建数据库。"
            }
            return@withContext secondError
        }

        firstError
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        sessionManager.close()
        preWarm()
    }

    override fun close() {
        sessionManager.close()
    }

    override fun consumeAutoRecoveryNotice(): String? {
        val notice = autoRecoveryNotice
        autoRecoveryNotice = null
        return notice
    }

    private suspend fun warmUpOnce(): Throwable? {
        return AppResult.runCatching("db.warmUp") {
            sessionManager.withDatabase { }
        }.fold(
            onSuccess = { null },
            onFailure = { it }
        )
    }

    private fun attemptAutoRecovery(): Boolean {
        return AppResult.runCatching("db.autoRecovery") {
            sessionManager.close()

            if (!snapshotDatabaseFiles()) {
                return@runCatching false
            }

            deleteDatabaseFiles()
            true
        }.onFailure {
            Logcat.e(TAG, "Auto recovery cleanup failed", it)
        }.getOrDefault(false)
    }

    private fun snapshotDatabaseFiles(): Boolean {
        val dbFile = appContext.getDatabasePath(DatabaseConfig.DATABASE_NAME)
        val parent = dbFile.parentFile ?: return false
        val filesToBackup = buildList {
            add(dbFile)
            add(File(parent, "${dbFile.name}-wal"))
            add(File(parent, "${dbFile.name}-shm"))
            add(File(parent, "${dbFile.name}-journal"))
        }.filter { it.exists() }

        if (filesToBackup.isEmpty()) {
            Logcat.w(TAG, "No database files found for snapshot; skip destructive recovery")
            return false
        }

        val recoveryDir = File(appContext.noBackupFilesDir, "db_recovery").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val copiedTargets = mutableListOf<File>()
        var snapshotFailed = false

        filesToBackup.forEach { source ->
            AppResult.runCatching("db.snapshot") {
                val target = File(recoveryDir, "${source.name}.$timestamp.bak")
                source.copyTo(target, overwrite = true)
                copiedTargets += target
            }.onFailure {
                Logcat.w(TAG, "Failed to snapshot DB file: ${source.name}", it)
                snapshotFailed = true
            }
        }

        if (snapshotFailed) return false

        val hasValidBackup = copiedTargets.all { it.exists() && it.length() > 0L }
        if (!hasValidBackup) {
            Logcat.e(TAG, "Snapshot integrity check failed (missing/empty backup file)")
            return false
        }

        val backups =
            recoveryDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return true
        backups.drop(MAX_RECOVERY_BACKUPS * 4).forEach { it.delete() }

        return true
    }

    private fun deleteDatabaseFiles() {
        val dbFile = appContext.getDatabasePath(DatabaseConfig.DATABASE_NAME)
        val parent = dbFile.parentFile

        appContext.deleteDatabase(DatabaseConfig.DATABASE_NAME)

        if (parent != null) {
            listOf(
                File(parent, "${dbFile.name}-wal"),
                File(parent, "${dbFile.name}-shm"),
                File(parent, "${dbFile.name}-journal")
            ).forEach { file ->
                if (file.exists() && !file.delete()) {
                    Logcat.w(TAG, "Failed to delete sidecar file: ${file.name}")
                }
            }
        }
    }

    private fun isRecoverableInitError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val name = current::class.java.simpleName
            val message = current.message.orEmpty().lowercase()
            if (name.contains("SQLiteNotADatabaseException", ignoreCase = true) ||
                message.contains("file is not a database") ||
                message.contains("file is encrypted") ||
                message.contains("passphrase") ||
                message.contains("password") ||
                message.contains("bad_decrypt") ||
                message.contains("tag mismatch")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}