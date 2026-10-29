package com.aozijx.passly.data.local.database.port

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VaultDatabaseResetController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: DatabaseSessionLifecycle,
    private val dataRefreshNotifier: EntryDataRefreshNotifier,
    private val logger: Logger,
) : DatabaseResetController {
    override suspend fun reset(): Throwable? = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val outcome = withContext(NonCancellable) {
            logger.inSpan(Operations.DatabaseReset) {
                var resetError: Throwable? = null
                var cancellation: CancellationException? = null
                try {
                    databaseSession.seal()
                    deleteDatabaseFiles()
                    VaultResourcePaths.RESOURCE_DIRECTORY_NAMES.forEach(::deleteVaultFileDirectory)
                } catch (cancelled: CancellationException) {
                    cancellation = cancelled
                } catch (error: Throwable) {
                    resetError = error
                }

                val reopenError = try {
                    databaseSession.unlock()
                } catch (cancelled: CancellationException) {
                    cancellation = cancellation ?: cancelled
                    null
                } catch (error: Throwable) {
                    error
                }
                if (reopenError == null) dataRefreshNotifier.notifyRefresh()

                if (resetError != null && reopenError != null && reopenError !== resetError) {
                    resetError.addSuppressed(reopenError)
                }
                val error = resetError ?: reopenError
                error?.let { runCatching { logger.log(LogStatements.Database.resetFailed(it)) } }
                ResetOutcome(error, cancellation)
            }
        }
        outcome.cancellation?.let { throw it }
        currentCoroutineContext().ensureActive()
        outcome.error
    }

    private data class ResetOutcome(
        val error: Throwable?,
        val cancellation: CancellationException?,
    )

    private fun deleteDatabaseFiles() {
        val databaseFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        val candidates = listOf(
            databaseFile,
            File(databaseFile.path + "-wal"),
            File(databaseFile.path + "-shm"),
            File(databaseFile.path + "-journal"),
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
        require(target.parentFile == filesRoot) { "Vault cleanup target escaped files directory" }
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Unable to delete Vault file directory: $name")
        }
    }
}
