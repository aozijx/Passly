package com.aozijx.passly.data.local.database.port

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VaultDatabaseResetController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val dataRefreshNotifier: EntryDataRefreshNotifier,
    private val telemetry: TelemetryReporter,
) : DatabaseResetController {
    override suspend fun reset(): Throwable? = withContext(Dispatchers.IO) {
        var resetError: Throwable? = null
        try {
            databaseSession.seal()
            deleteDatabaseFiles()
            VaultResourcePaths.RESOURCE_DIRECTORY_NAMES.forEach(::deleteVaultFileDirectory)
        } catch (error: Throwable) {
            resetError = error
            telemetry.report(
                EventLevel.ERROR,
                EventCategory.DATABASE,
                "database.reset_cleanup_failed",
                error,
            )
        }

        val reopenError = databaseSession.unlock()
        if (reopenError == null) dataRefreshNotifier.notifyRefresh()
        resetError ?: reopenError
    }

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