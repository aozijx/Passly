package com.aozijx.passly.core.session

import androidx.room.withTransaction
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.domain.authentication.SessionLockedException
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.repository.database.TransactionOperator
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 统一会话管理器（策略层）。
 *
 * 唯一真实来源，持有 AppDatabase 实例引用和锁状态。
 *
 * API 分三类：
 * - [query]：即查即返，用于一次性的挂起查询，计数只在执行期间生效
 * - [transaction]：写事务，自动包裹 Room 事务边界 + 自动回滚
 * - [observeFlow]：持续观察，计数包裹整个 Flow 收集生命周期，取消收集时自动释放
 *
 * 锁定流程 ([lock]) 包含超时熔断机制：超时后强制取消僵尸协程 ([operationScope])。
 */
@Singleton
class UnifiedSessionManager @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val dekManager: DekManager
) : SessionStateProvider, TransactionOperator {

    private companion object {
        private const val TAG = "UnifiedSessionManager"
        private const val DRAIN_TIMEOUT_MS = 5000L
    }

    /** 用于强制取消僵尸事务的协程作用域。所有 DB 操作均在此作用域内启动。 */
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeOps = AtomicInteger(0)
    private val lockState = AtomicReference(LockState.LOCKED)

    private val mutex = Mutex()
    private var database: AppDatabase? = null

    // ============================== 公共 API ==============================

    /**
     * 即查即返（一次性挂起查询）。
     * 计数仅在方法执行期间生效，返回值后立即释放。
     */
    suspend fun <T> query(block: suspend AppDatabase.() -> T): T {
        return executeWithGuard(block)
    }

    /**
     * 写事务（强制包裹 Room 事务边界）。
     * 使用 [AppDatabase.withTransaction] 自动管理 begin/setSuccessful/endTransaction。
     */
    suspend fun <T> transaction(block: suspend AppDatabase.() -> T): T {
        return executeWithGuard { db ->
            db.withTransaction { block(db) }
        }
    }

    /**
     * 持续观察（专用于 Flow）。
     *
     * 返回的 Flow 在每次 [collect] 时：
     * 1. 递增活跃计数 + 检查锁状态
     * 2. 获取数据库实例，调用 [block] 获取上游 Flow
     * 3. 透传上游 Flow 的所有元素
     * 4. [collect] 取消或异常终止时递减计数
     *
     * 这样 [lock] 中的排干逻辑能正确等待所有正在收集的 Flow 完成。
     */
    fun <T> observeFlow(block: suspend AppDatabase.() -> Flow<T>): Flow<T> = flow {
        activeOps.incrementAndGet()
        try {
            ensureNotLocked()
            val db = resolveDatabase()
            // 使用 collect 替代 emitAll，以便在每次发射前检查锁状态
            db.block().collect { value ->
                ensureNotLocked()
                emit(value)
            }
        } finally {
            activeOps.decrementAndGet()
        }
    }.flowOn(Dispatchers.IO)

    // ============================== SessionStateProvider ==============================

    override fun assertWritable() {
        ensureNotLocked()
    }

    override fun <T> trackReadOperation(block: () -> T): T {
        activeOps.incrementAndGet()
        try {
            return block()
        } finally {
            activeOps.decrementAndGet()
        }
    }

    // ============================== TransactionOperator ==============================

    override suspend fun <T> withTransaction(block: suspend () -> T): T {
        return executeWithGuard { db ->
            db.withTransaction { block() }
        }
    }

    override suspend fun <T> withNewTransaction(block: suspend () -> T): T {
        // Room 的 withTransaction 已支持嵌套 —— 内部若检测到已有事务，会挂起等待
        // 新事务可用；这里直接委托给 withTransaction（Room 自动处理传播）
        // 如需强制独立事务，可使用 runInTransaction 并传递独立连接。
        return executeWithGuard { db ->
            db.withTransaction { block() }
        }
    }

    // ============================== 锁管理 ==============================

    /**
     * 解锁会话。允许新的 [query] / [transaction] / [observeFlow] 操作。
     */
    fun unlock() {
        lockState.set(LockState.UNLOCKED)
        AppLog.i(TAG, "Session unlocked")
    }

    /**
     * 锁定会话。
     *
     * 1. 设置 [LockState.LOCKING] 状态阻止新操作
     * 2. 等待活跃操作排干（最多 [timeout] 时间）
     * 3. 若超时：强制取消 [operationScope] 取消僵尸协程，重置计数
     * 4. 设置 [LockState.LOCKED] 状态
     *
     * **不关闭数据库连接** —— 物理连接与进程生命周期绑定。
     */
    suspend fun lock(timeout: Duration = 5.seconds) {
        lockState.set(LockState.LOCKING)

        val drained = withTimeoutOrNull(timeout) {
            while (activeOps.get() > 0) {
                kotlinx.coroutines.delay(50)
            }
        }

        if (drained == null) {
            // 超时熔断：强制杀死僵尸协程
            AppLog.w(TAG, "Lock drain timeout, cancelling zombie operations")
            operationScope.coroutineContext[Job]?.let { job ->
                job.children.forEach { it.cancel() }
            }
            // 兜底重置计数
            activeOps.set(0)
            AppLog.e(TAG, "Forced rollback completed, ${activeOps.get()} active ops remaining")
        }

        lockState.set(LockState.LOCKED)
        AppLog.i(TAG, "Session locked (database connection preserved)")
    }

    // ============================== 数据库生命周期 ==============================

    /**
     * 打开数据库连接。
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

    private suspend fun <T> executeWithGuard(
        block: suspend (AppDatabase) -> T
    ): T {
        // 在 operationScope 内启动，使 lock() 超时时能通过 cancelChildren 取消僵尸操作
        return operationScope.async(Dispatchers.IO) {
            activeOps.incrementAndGet()
            try {
                ensureNotLocked()
                val db = resolveDatabase()
                block(db)
            } finally {
                activeOps.decrementAndGet()
            }
        }.await()
    }

    private fun ensureNotLocked() {
        when (lockState.get()) {
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
