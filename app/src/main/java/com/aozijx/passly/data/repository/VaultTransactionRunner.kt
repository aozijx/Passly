package com.aozijx.passly.data.repository

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.reporting.AppErrorReporter
import com.aozijx.passly.core.telemetry.reporting.ErrorReportContext
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一事务运行器。
 *
 * 职责：
 * - 包裹数据库事务边界（通过 [UnifiedSessionManager.transaction]）
 * - 包裹读操作（通过 [UnifiedSessionManager.query]）
 * - 供应 [AppDatabase] 接收者作用域，供内部 lambda 安全访问 DAO
 * - 错误映射（乐观锁冲突等）
 *
 * Command Handler 通过此类间接访问数据库，不直接引用 DAO、Room、withTransaction。
 *
 * 读/写操作的租约管理由 [UnifiedSessionManager] 负责，本类不再额外调用 [SessionStateProvider]。
 */
@Singleton
class VaultTransactionRunner @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val errorReporter: AppErrorReporter
) {

    /**
     * 在写事务中执行 [block]。
     * 错误捕获在外层，确保异常先触发 Room 事务回滚，再转为 [AppResult.Failure]。
     */
    suspend fun <T> write(
        operation: String,
        block: suspend AppDatabase.() -> T
    ): AppResult<T> {
        val result = AppResult.runSuspendCatching {
            requireFullSecureSessionAccess()
            sessionManager.transaction {
                block()
            }
        }
        return result.onFailure { report(it, operation) }
    }

    /**
     * 在读操作中执行 [block]。
     */
    suspend fun <T> read(
        operation: String,
        block: suspend AppDatabase.() -> T
    ): AppResult<T> {
        val result = AppResult.runSuspendCatching {
            requireFullSecureSessionAccess()
            sessionManager.query {
                block()
            }
        }
        return result.onFailure { report(it, operation) }
    }

    /**
     * 检查乐观锁版本是否匹配，不匹配时抛出 [Conflict]。
     */
    fun checkVersion(actualVersion: Int, expectedVersion: Int) {
        if (actualVersion != expectedVersion) {
            throw Conflict()
        }
    }

    /**
     * 检查乐观锁影响行数，0 行表示版本冲突。
     */
    fun checkAffectedRows(affectedRows: Int) {
        if (affectedRows == 0) {
            throw Conflict()
        }
    }

    private fun requireFullSecureSessionAccess() {
        if (!sessionState.hasFullSecureSessionAccess()) {
            throw SessionModeRestricted()
        }
    }

    private fun report(error: com.aozijx.passly.core.error.model.AppError, operation: String) {
        errorReporter.report(
            error = error,
            context = ErrorReportContext(
                operation = OperationCode(operation),
                category = EventCategory.DATABASE
            )
        )
    }
}
