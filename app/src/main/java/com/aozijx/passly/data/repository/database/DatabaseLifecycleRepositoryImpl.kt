package com.aozijx.passly.data.repository.database

import android.content.Context
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DatabaseLifecycleRepositoryImpl(
    context: Context
) : DatabaseLifecycleRepository {
    private companion object {
        private const val TAG = "DatabaseLifecycle"
    }

    private val appContext = context.applicationContext

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        val firstError = warmUpOnce() ?: return@withContext null

        // In debug builds, auto-heal legacy/plain/corrupted DB files once.
        if (BuildConfig.DEBUG && isNotDatabaseError(firstError)) {
            Logcat.w(
                TAG,
                "Detected non-database file, attempting one-time auto recovery",
                firstError
            )
            AppDatabase.reset()
            appContext.deleteDatabase(DatabaseConfig.DATABASE_NAME)
            return@withContext warmUpOnce()
        }

        firstError
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        AppDatabase.reset()
        preWarm()
    }

    override fun close() {
        AppDatabase.close()
    }

    private fun warmUpOnce(): Throwable? {
        return runCatching { AppDatabase.preWarm(appContext) }
            .fold(
                onSuccess = { AppDatabase.initializationError },
                onFailure = { it }
            )
    }

    private fun isNotDatabaseError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val name = current::class.java.simpleName
            val message = current.message.orEmpty()
            if (name.contains("SQLiteNotADatabaseException", ignoreCase = true) ||
                message.contains("file is not a database", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}