package com.aozijx.passly.domain.diagnostics.usecase

import com.aozijx.passly.domain.diagnostics.repository.DatabaseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DatabaseInitOutcome(
    val error: Throwable? = null
) {
    val success: Boolean get() = error == null
}

@Singleton
class DatabaseLifecycleUseCases @Inject constructor(
    private val repository: DatabaseController
) {
    /** 具体的重试逻辑由 Repository 实现。 */
    suspend fun preWarmAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        DatabaseInitOutcome(error = repository.preWarm())
    }

    /**
     * 重置数据库后再次预热。
     */
    suspend fun retryAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        DatabaseInitOutcome(error = repository.retry())
    }

    suspend fun preWarm(): Throwable? = repository.preWarm()

    suspend fun retry(): Throwable? = repository.retry()

    suspend fun clearAndReinitialize(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        DatabaseInitOutcome(error = repository.clearAndReinitialize())
    }

    /**
     * 关闭底层数据库连接。
     */
    suspend fun close() = repository.close()
}
