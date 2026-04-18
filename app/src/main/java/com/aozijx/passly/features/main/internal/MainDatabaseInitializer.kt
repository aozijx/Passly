package com.aozijx.passly.features.main.internal

import com.aozijx.passly.domain.usecase.database.DatabaseLifecycleUseCases

internal data class MainDatabaseInitResult(
    val error: Throwable?
)

internal class MainDatabaseInitializer(
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases
) {

    suspend fun initialize(): MainDatabaseInitResult {
        val error = databaseLifecycleUseCases.preWarm()
        return MainDatabaseInitResult(error = error)
    }

    suspend fun retry(): MainDatabaseInitResult {
        val error = databaseLifecycleUseCases.retry()
        return MainDatabaseInitResult(error = error)
    }
}
