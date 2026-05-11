package com.aozijx.passly.features.main.internal

import com.aozijx.passly.domain.usecase.database.DatabaseLifecycleUseCases

internal data class MainDatabaseInitResult(
    val error: Throwable?,
    val recoveryNotice: String? = null
)

internal class MainDatabaseInitializer(
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases
) {

    suspend fun initialize(): MainDatabaseInitResult {
        val error = databaseLifecycleUseCases.preWarm()
        val notice =
            if (error == null) databaseLifecycleUseCases.consumeAutoRecoveryNotice() else null
        return MainDatabaseInitResult(error = error, recoveryNotice = notice)
    }

    suspend fun retry(): MainDatabaseInitResult {
        val error = databaseLifecycleUseCases.retry()
        val notice =
            if (error == null) databaseLifecycleUseCases.consumeAutoRecoveryNotice() else null
        return MainDatabaseInitResult(error = error, recoveryNotice = notice)
    }
}