package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.domain.repository.database.DatabaseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责打开、探测、重试和关闭加密数据库会话。
 * 包含内部重试机制，以处理认证/锁定状态切换时的瞬时竞争。
 */
@Singleton
internal class DatabaseControllerImpl @Inject constructor(
    private val session: DatabaseSession
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

            // 如果是锁定导致的错误，由于 DatabaseSession 已经有等待机制，
            // 这里的重试更多是为了处理 Room 内部连接池初始化等其他瞬时故障。
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
        session.closeAndAwait()
        preWarm()
    }

    override suspend fun close() {
        session.closeAndAwait()
    }

    private suspend fun probe(): Throwable? =
        AppResult.runSuspendCatching("db.warmUp") {
            session.withDatabase { openHelper.writableDatabase }
        }.fold(
            onSuccess = { null },
            onFailure = { it }
        )
}
