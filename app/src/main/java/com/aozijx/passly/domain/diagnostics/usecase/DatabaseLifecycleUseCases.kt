package com.aozijx.passly.domain.diagnostics.usecase

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.fromThrowable
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
    /**
     * 在 IO 调度器上预热数据库，封装错误处理与日志。
     * 具体的重试逻辑已下沉至 Repository 实现层。
     */
    suspend fun preWarmAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        val error = repository.preWarm()
        if (error == null) {
            AppLog.i(TAG, "Database preWarm completed")
        } else {
            AppLog.e(
                TAG,
                "Database preWarm failed",
                AppError.fromThrowable(
                    error,
                    ErrorLayer.DATA,
                    "database.preWarm"
                )
            )
        }
        DatabaseInitOutcome(error = error)
    }

    /**
     * 重置数据库后再次预热。
     */
    suspend fun retryAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        val error = repository.retry()
        if (error == null) {
            AppLog.i(TAG, "Database retry completed")
        } else {
            AppLog.e(
                TAG,
                "Database retry failed",
                AppError.fromThrowable(
                    error,
                    ErrorLayer.DATA,
                    "database.retry"
                )
            )
        }
        DatabaseInitOutcome(error = error)
    }

    suspend fun preWarm(): Throwable? = repository.preWarm()

    suspend fun retry(): Throwable? = repository.retry()

    /**
     * 关闭底层数据库连接。
     */
    suspend fun close() = repository.close()

    private companion object {
        private const val TAG = "DatabaseLifecycle"
    }
}
