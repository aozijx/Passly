package com.aozijx.passly.app.database

import com.aozijx.passly.data.local.database.port.DatabaseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DatabaseLifecycleResult {
    data object Ready : DatabaseLifecycleResult

    data class Reinitialized(val recoveryId: String?) : DatabaseLifecycleResult

    data class Failure(val cause: Throwable) : DatabaseLifecycleResult
}

@Singleton
class DatabaseLifecycleGateway @Inject constructor(
    private val controller: DatabaseController,
) {
    suspend fun initialize(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        controller.preWarm().toLifecycleResult()
    }

    suspend fun retry(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        controller.retry().toLifecycleResult()
    }

    suspend fun quarantineAndReinitialize(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        runCatching { controller.quarantineAndReinitialize() }.fold(
            onSuccess = { result ->
                result.error?.let(DatabaseLifecycleResult::Failure)
                    ?: DatabaseLifecycleResult.Reinitialized(result.recoveryId)
            },
            onFailure = DatabaseLifecycleResult::Failure,
        )
    }

    suspend fun clearAndReinitialize(): DatabaseLifecycleResult = withContext(Dispatchers.IO) {
        controller.clearAndReinitialize().toLifecycleResult()
    }

    suspend fun close() = controller.close()

    private fun Throwable?.toLifecycleResult(): DatabaseLifecycleResult =
        this?.let(DatabaseLifecycleResult::Failure) ?: DatabaseLifecycleResult.Ready
}
