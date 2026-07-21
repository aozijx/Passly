package com.aozijx.passly.core.session

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.session.UnifiedSessionManager.Companion.DRAIN_TIMEOUT_MS
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一会话管理器（策略层）。
 *
 * 唯一真实来源：
 * - 持有 AppDatabase 实例引用（进程级生命周期，锁定时不关闭）
 * - 持有锁状态（LOCKED / LOCKING / UNLOCKED）
 * - 引用计数追踪活跃操作
 *
 * 分层职责：
 * - [read]：读操作，执行前检查锁状态
 * - [write]：写操作，执行前检查锁状态
 * - [lock] / [unlock]：锁定/解锁，锁定**不关闭**数据库连接
 * - [closeDatabase]：仅在应用进入后台或销毁时调用
 */
@Singleton
class UnifiedSessionManager @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val dekManager: DekManager
) {

    private companion object {
        private const val TAG = "UnifiedSessionManager"
        private const val DRAIN_TIMEOUT_MS = 5000L
    }

    private val _lockState = MutableStateFlow(LockState.LOCKED)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val mutex = Mutex()
    private var database: AppDatabase? = null
    private val activeOps = AtomicInteger(0)

    // ============================== 公共 API ==============================

    /**
     * 执行读操作。锁定状态下抛出 [SessionLockedException]。
     */
    suspend fun <T> read(block: suspend AppDatabase.() -> T): T {
        ensureUnlocked()
        val db = resolveDatabase()
        activeOps.incrementAndGet()
        return try {
            withContext(Dispatchers.IO) { block(db) }
        } finally {
            activeOps.decrementAndGet()
        }
    }

    /**
     * 执行写操作。语义同 [read]，用于标识写入意图以供未来事务管理。
     * 注意：不自动包裹 withTransaction，Repository 层自行管理事务边界。
     */
    suspend fun <T> write(block: suspend AppDatabase.() -> T): T {
        ensureUnlocked()
        val db = resolveDatabase()
        activeOps.incrementAndGet()
        return try {
            withContext(Dispatchers.IO) { block(db) }
        } finally {
            activeOps.decrementAndGet()
        }
    }

    /**
     * 解锁会话。允许新的 read/write 操作。
     * 由 VaultSessionController 在认证成功后调用。
     */
    fun unlock() {
        _lockState.value = LockState.UNLOCKED
        AppLog.i(TAG, "Session unlocked")
    }

    /**
     * 锁定会话。
     * 1. 设置 LOCKING 状态阻止新操作
     * 2. 等待活跃操作排干（最多 [DRAIN_TIMEOUT_MS]ms）
     * 3. 设置 LOCKED 状态
     *
     * **不关闭数据库连接** —— 物理连接与进程生命周期绑定。
     */
    suspend fun lock() {
        _lockState.value = LockState.LOCKING

        val deadline = System.currentTimeMillis() + DRAIN_TIMEOUT_MS
        while (activeOps.get() > 0) {
            if (System.currentTimeMillis() > deadline) {
                AppLog.w(TAG, "Lock drain timeout, ${activeOps.get()} ops still active")
                break
            }
            // 等待活跃操作完成
            kotlinx.coroutines.delay(50)
        }

        _lockState.value = LockState.LOCKED
        AppLog.i(TAG, "Session locked (database connection preserved)")
    }

    /**
     * 打开数据库连接。仅在首次解锁后或 closeDatabase 后调用。
     * 使用 [DekManager.withDek] 获取加密密钥。
     */
    suspend fun openDatabase(): Throwable? {
        return try {
            val dek = dekManager.withDek { it.clone() }
            val db = databaseProvider.open(dek)
            dek.fill(0)
            mutex.withLock { database = db }
            AppLog.i(TAG, "Database connection opened")
            null
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to open database", e)
            e
        }
    }

    /**
     * 关闭数据库连接。
     * **仅**在应用进入后台（onStop）或销毁（onDestroy）时调用。
     * **绝对禁止**在锁定流程中调用。
     */
    suspend fun closeDatabase() {
        mutex.withLock {
            database?.let { db ->
                runCatching { db.close() }
                    .onFailure { e -> AppLog.e(TAG, "Database close error", e) }
                database = null
                AppLog.i(TAG, "Database connection closed")
            }
        }
    }

    // ============================== 内部方法 ==============================

    private fun ensureUnlocked() {
        when (_lockState.value) {
            LockState.UNLOCKED -> { /* ok */
            }

            LockState.LOCKING -> throw SessionLockedException("Vault session is locking")
            LockState.LOCKED -> throw SessionLockedException("Vault session is locked")
        }
    }

    /**
     * 获取当前数据库实例。
     * 如果尚未打开，则自动打开（首次使用时惰性初始化）。
     */
    private suspend fun resolveDatabase(): AppDatabase {
        mutex.withLock {
            database?.let { return it }

            // 惰性初始化：首次访问时打开
            val dek = dekManager.withDek { it.clone() }
            val db = databaseProvider.open(dek)
            dek.fill(0)
            database = db
            return db
        }
    }
}
