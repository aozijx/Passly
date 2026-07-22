package com.aozijx.passly.data.repository

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.Conflict
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.authentication.SessionStateProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一事务入口。
 *
 * 职责：
 * - 包裹数据库事务边界（通过 [UnifiedSessionManager.transaction]）
 * - 包裹读操作（通过 [UnifiedSessionManager.query]）
 * - 供应 [AppDatabase] 接收者作用域，供内部 lambda 安全访问 DAO
 * - 错误映射（乐观锁冲突等）
 *
 * Command Handler 通过此类间接访问数据库，不直接引用 DAO、Room、withTransaction。
 */
@Singleton
class VaultUnitOfWork @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager
) {

    /**
     * 在写事务中执行 [block]。
     * 错误捕获在外层，确保异常先触发 Room 事务回滚，再转为 [AppResult.Failure]。
     */
    suspend fun <T> write(
        operation: String,
        block: suspend AppDatabase.() -> T
    ): AppResult<T> {
        stateProvider.assertWritable()
        return AppResult.runSuspendCatching(operation) {
            sessionManager.transaction {
                block()
            }
        }
    }

    /**
     * 在读操作中执行 [block]。
     */
    suspend fun <T> read(
        operation: String,
        block: suspend AppDatabase.() -> T
    ): AppResult<T> {
        stateProvider.assertWritable()
        return AppResult.runSuspendCatching(operation) {
            sessionManager.query {
                block()
            }
        }
    }

    /**
     * 检查乐观锁版本是否匹配，不匹配时抛出 [Conflict]。
     */
    fun checkVersion(entryId: String, actualVersion: Int, expectedVersion: Int) {
        if (actualVersion != expectedVersion) {
            throw Conflict(
                "entry:$entryId version mismatch: expected=$expectedVersion, actual=$actualVersion"
            )
        }
    }

    /**
     * 检查乐观锁影响行数，0 行表示版本冲突。
     */
    fun checkAffectedRows(entryId: String, expectedVersion: Int, affectedRows: Int) {
        if (affectedRows == 0) {
            throw Conflict(
                "entry:$entryId optimistic lock failed: expected version=$expectedVersion"
            )
        }
    }
}
