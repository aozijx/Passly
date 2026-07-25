package com.aozijx.passly.data.repository.database

import android.content.Context
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.maintenance.DatabaseRecoveryStore
import com.aozijx.passly.domain.diagnostics.repository.DatabaseController
import com.aozijx.passly.domain.diagnostics.repository.DatabaseQuarantineResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责打开、探测、重试和关闭加密数据库会话。
 * 包含内部重试机制，以处理解锁状态的瞬时竞争。
 */
@Singleton
internal class DatabaseControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: UnifiedSessionManager,
    private val recoveryStore: DatabaseRecoveryStore
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
                if (i > 1) AppTelemetry.i(TAG, "Database preWarm succeeded after $i attempts")
                return@withContext null
            }

            if (i < MAX_RETRIES) {
                AppTelemetry.w(
                    TAG,
                    "Database probe failed (attempt $i/$MAX_RETRIES), retrying... ${lastError.message}"
                )
                delay(RETRY_DELAY_MS)
            }
        }
        lastError
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        sessionManager.seal()
        sessionManager.unlock()
    }

    override suspend fun quarantineAndReinitialize(): DatabaseQuarantineResult =
        withContext(Dispatchers.IO) {
            sessionManager.seal()
            val recoveryId = recoveryStore.preserveAndClearActiveVault()
            DatabaseQuarantineResult(
                recoveryId = recoveryId,
                error = sessionManager.unlock()
            )
        }

    override suspend fun clearAndReinitialize(): Throwable? = withContext(Dispatchers.IO) {
        var recoveryError: Throwable? = null
        try {
            sessionManager.seal()
            deleteDatabaseFiles()
            deleteVaultFileDirectory("attachments")
            deleteVaultFileDirectory("vault_images")
        } catch (error: Throwable) {
            recoveryError = error
            AppTelemetry.e(TAG, "Database recovery cleanup failed", error)
        }

        val reopenError = sessionManager.unlock()
        recoveryError ?: reopenError
    }

    override suspend fun close() {
        sessionManager.closeDatabase()
    }

    private suspend fun probe(): Throwable? =
        AppResult.runSuspendCatching("db.warmUp") {
            sessionManager.query { openHelper.writableDatabase }
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
}
