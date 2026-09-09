package com.aozijx.passly.app.database

import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.port.DatabaseController
import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DatabaseLifecycleResult {
    data object Ready : DatabaseLifecycleResult

    data class Failure(val cause: Throwable) : DatabaseLifecycleResult
}

@Singleton
class DatabaseLifecycleGateway @Inject constructor(
    private val controller: DatabaseController,
) : DatabaseResetGateway {
    suspend fun initialize(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        controller.preWarm().toLifecycleResult()
    }

    suspend fun retry(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        controller.retry().toLifecycleResult()
    }

    override suspend fun reset(): AppResult<Unit> = withContext(Dispatchers.IO) {
        controller.reset()?.let { AppResult.Failure(AppError.fromThrowable(it)) }
            ?: AppResult.Success(Unit)
    }

    suspend fun close() = controller.close()

    private fun Throwable?.toLifecycleResult(): DatabaseLifecycleResult =
        this?.let(DatabaseLifecycleResult::Failure) ?: DatabaseLifecycleResult.Ready
}
