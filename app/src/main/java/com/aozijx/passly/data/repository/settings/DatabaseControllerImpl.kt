package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.domain.repository.database.DatabaseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens, probes, retries, and closes the encrypted database session.
 *
 * Initialization failures are reported to the caller. This class deliberately
 * never deletes database files: an invalid key and a corrupt schema must not be
 * converted into silent data loss.
 */
@Singleton
internal class DatabaseControllerImpl @Inject constructor(
    private val session: DatabaseSession
) : DatabaseController {

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        probe()
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        session.closeAndAwait()
        probe()
    }

    override suspend fun close() {
        session.closeAndAwait()
    }

    private suspend fun probe(): Throwable? =
        AppResult.runSuspendCatching("db.warmUp") {
            session.withDatabase { openHelper.writableDatabase }
        }.fold(
            onSuccess = { null },
            onFailure = { it }
        )
}
