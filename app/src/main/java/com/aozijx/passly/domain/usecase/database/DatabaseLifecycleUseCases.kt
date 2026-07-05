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
     * 在 IO 调度器上预热数据库，封装错误处理与日志，避免调用方关心线程切换。
     * 返回的 [DatabaseInitOutcome] 同时承载初始化结果与一次性自动恢复提示。
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
     * 重置数据库后再次预热，复用与 [preWarmAndReport] 相同的封装。
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

    /**
     * 保留原有的细粒度 API，便于其他用例（不关心完整初始化报告的场景）继续使用。
     */
    suspend fun preWarm(): Throwable? = repository.preWarm()

    suspend fun retry(): Throwable? = repository.retry()

    fun consumeAutoRecoveryNotice(): String? = repository.consumeAutoRecoveryNotice()

    fun close() = repository.close()

    private companion object {
        private const val TAG = "DatabaseLifecycle"
    }
}