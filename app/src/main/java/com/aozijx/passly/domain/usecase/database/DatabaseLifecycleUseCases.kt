package com.aozijx.passly.domain.usecase.database

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DatabaseInitOutcome(
    val error: Throwable? = null,
    val recoveryNotice: String? = null
) {
    val success: Boolean get() = error == null
}

@Singleton
class DatabaseLifecycleUseCases @Inject constructor(
    private val repository: DatabaseLifecycleRepository
) {
    /**
     * 在 IO 调度器上预热数据库，封装错误处理与日志。
     */
    suspend fun preWarmAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        val error = repository.preWarm()
        val notice = if (error == null) repository.consumeAutoRecoveryNotice() else null
        if (error == null) {
            Logcat.i(TAG, "Database preWarm completed")
        } else {
            Logcat.e(
                TAG,
                "Database preWarm failed",
                AppError.fromThrowable(
                    error,
                    com.aozijx.passly.core.error.ErrorLayer.DATA,
                    "database.preWarm"
                )
            )
        }
        DatabaseInitOutcome(error = error, recoveryNotice = notice)
    }

    /**
     * 重置数据库后再次预热。
     */
    suspend fun retryAndReport(): DatabaseInitOutcome = withContext(Dispatchers.IO) {
        val error = repository.retry()
        val notice = if (error == null) repository.consumeAutoRecoveryNotice() else null
        if (error == null) {
            Logcat.i(TAG, "Database retry completed")
        } else {
            Logcat.e(
                TAG,
                "Database retry failed",
                AppError.fromThrowable(
                    error,
                    com.aozijx.passly.core.error.ErrorLayer.DATA,
                    "database.retry"
                )
            )
        }
        DatabaseInitOutcome(error = error, recoveryNotice = notice)
    }

    suspend fun preWarm(): Throwable? = repository.preWarm()

    suspend fun retry(): Throwable? = repository.retry()

    fun consumeAutoRecoveryNotice(): String? = repository.consumeAutoRecoveryNotice()

    /**
     * 关闭底层数据库连接。现在是挂起函数，确保关闭完成后才返回。
     */
    suspend fun close() = repository.close()

    private companion object {
        private const val TAG = "DatabaseLifecycle"
    }
}