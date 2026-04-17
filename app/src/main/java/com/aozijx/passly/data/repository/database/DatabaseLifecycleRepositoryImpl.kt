package com.aozijx.passly.data.repository.database

import android.content.Context
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DatabaseLifecycleRepositoryImpl(
    context: Context
) : DatabaseLifecycleRepository {

    private val appContext = context.applicationContext

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        runCatching { AppDatabase.preWarm(appContext) }
            .fold(
                onSuccess = { AppDatabase.initializationError },
                onFailure = { it }
            )
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        AppDatabase.reset()
        preWarm()
    }

    override fun close() {
        AppDatabase.close()
    }
}
