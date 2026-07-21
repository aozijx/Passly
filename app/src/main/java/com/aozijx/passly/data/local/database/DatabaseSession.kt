package com.aozijx.passly.data.local.database

import com.aozijx.passly.core.session.UnifiedSessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库会话（兼容层）。
 *
 * 提供与旧代码兼容的 [withDatabase] 作用域，内部委托给 [UnifiedSessionManager]。
 * 不再管理锁定/解锁或数据库关闭 —— 这些职责已上移至 [UnifiedSessionManager]。
 */
@Singleton
class DatabaseSession @Inject constructor(
    private val sessionManager: UnifiedSessionManager
) {

    /**
     * 在数据库上下文中执行操作。
     * 内部委托给 [UnifiedSessionManager.read]。
     */
    suspend fun <T> withDatabase(block: suspend AppDatabase.() -> T): T {
        return sessionManager.read(block)
    }
}
