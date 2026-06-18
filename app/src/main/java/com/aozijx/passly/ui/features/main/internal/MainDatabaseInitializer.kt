package com.aozijx.passly.ui.features.main.internal

import com.aozijx.passly.domain.usecase.database.DatabaseLifecycleUseCases

import javax.inject.Inject
import javax.inject.Singleton

data class MainDatabaseInitResult(
    val error: Throwable?,
    val recoveryNotice: String? = null
)

@Singleton
class MainDatabaseInitializer @Inject constructor(
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