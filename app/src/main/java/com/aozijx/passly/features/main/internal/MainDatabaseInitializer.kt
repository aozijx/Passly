package com.aozijx.passly.features.main.internal

import android.content.Context
import com.aozijx.passly.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MainDatabaseInitResult(
    val error: Throwable?
)

internal class MainDatabaseInitializer {

    suspend fun initialize(context: Context): MainDatabaseInitResult = withContext(Dispatchers.IO) {
        runCatching {
            AppDatabase.preWarm(context)
            AppDatabase.initializationError
        }.fold(
            onSuccess = { MainDatabaseInitResult(error = it) },
            onFailure = { MainDatabaseInitResult(error = it) }
        )
    }
}
