package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.domain.repository.database.DatabaseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责打开、探测、重试和关闭加密数据库会话。
 * 包含内部重试机制，以处理解锁状态的瞬时竞争。
 */
@Singleton
internal class DatabaseControllerImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager
) : DatabaseController {

    private companion object {
        private const val TAG = "DatabaseController"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 500L
    }

    override suspend fun preWarm(): Throwable? = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (i in 1..MAX_RETRIES) {
            lastError = probe()
            if (lastError == null) {
                if (i > 1) AppLog.i(TAG, "Database preWarm succeeded after $i attempts")
                return@withContext null
            }

            if (i < MAX_RETRIES) {
                AppLog.w(
                    TAG,
                    "Database probe failed (attempt $i/$MAX_RETRIES), retrying... ${lastError.message}"
                )
                delay(RETRY_DELAY_MS)
            }
        }
        lastError
    }

    override suspend fun retry(): Throwable? = withContext(Dispatchers.IO) {
        // 关闭旧连接后重新预热（用于初始化失败后的用户重试）
        sessionManager.closeDatabase()
        preWarm()
    }

    override suspend fun close() {
        sessionManager.closeDatabase()
    }

    private suspend fun probe(): Throwable? =
        AppResult.runSuspendCatching("db.warmUp") {
            sessionManager.read { openHelper.writableDatabase }
        }.fold(
            onSuccess = { null },
            onFailure = { it }
        )
}
