package com.aozijx.passly.core.session

import androidx.room.withTransaction
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.domain.authentication.SessionLockedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 数据库租约闸门。
 *
 * 单一真实来源，统一管理数据库访问租约和锁状态。
 * 替代旧的 [assertWritable] + [trackReadOperation] + [activeOps.set(0)] 分散式设计。
 *
 * ## 租约模型
 * - [withReadLease]：一次性挂起读，租约覆盖 block 执行期
 * - [withWriteLease]：写事务，自动包裹 Room 事务边界
 * - [observeWithLease]：持续 Flow 观察，租约覆盖整个 collect 生命周期
 * - 每个租约只释放一次，不允许人工重置计数
 *
 * ## 锁状态
 * - **SOFT_LOCKED**：阻止新租约，数据库保持打开。应用层访问控制，
 *   SQLCipher 连接仍持有解密能力，不是完整密码学封存。
 * - **SEALED**：阻止新租约、排干活跃租约、关闭数据库。
 * - **UNLOCKED**：正常运行。
 *
 * ## 锁流程
 * ```
 * [seal]      → 状态 SEALED → 排干租约（超时熔断）→ 关闭数据库
 * [softLock]  → 状态 SOFT_LOCKED（阻止新租约，不关库）
 * [unlock]    → 打开数据库（若未打开）→ 状态 UNLOCKED
 * ```
 *
 * SEALED → UNLOCKED 需要 DEK 重新打开数据库；
 * SOFT_LOCKED → UNLOCKED 仅改变状态，不重新打开数据库。
 */
@Singleton
class DatabaseLeaseGate @Inject constructor(
    private val databaseProvider: DatabaseProvider
) {
    private companion object {
        private const val TAG = "DatabaseLeaseGate"
        private const val DRAIN_POLL_MS = 50L
        private const val POST_CANCEL_DELAY_MS = 100L
    }

    // ============================== 状态 ==============================

    private val _lockState = MutableStateFlow(LockState.SEALED)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val activeLeases = AtomicInteger(0)
    private val leaseMutex = Mutex()

    private val dbMutex = Mutex()

    @Volatile
    private var database: AppDatabase? = null

    /** Flow 观察的独立作用域，[seal] 超时时取消此作用域以强制终止。 */
    private val flowScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ============================== 公共 API ==============================

    /**
     * 一次性读租约。
     *
     * 获取租约 → 检查状态 → 执行 [block] → 释放租约。
     * 若已锁定（[SOFT_LOCKED] 或 [SEALED]），立即抛出 [SessionLockedException]。
     */
    suspend fun <T> withReadLease(block: suspend AppDatabase.() -> T): T {
        acquireLease()
        return try {
            val db = resolveDatabase()
            block(db)
        } finally {
            releaseLease()
        }
    }

    /**
     * 写事务租约。
     *
     * 自动包裹 Room 事务边界。语义同 [withReadLease]，区别在于 block 在
     * [AppDatabase.withTransaction] 内执行。
     */
    suspend fun <T> withWriteLease(block: suspend AppDatabase.() -> T): T {
        acquireLease()
        return try {
            val db = resolveDatabase()
            db.withTransaction { block(db) }
        } finally {
            releaseLease()
        }
    }

    /**
     * 持续观察租约（专用于 Flow）。
     *
     * 返回的 Flow 在每次 [collect] 时：
     * 1. 获取租约 + 检查状态
     * 2. 获取数据库实例，调用 [block] 获取上游 Flow
     * 3. 透传上游元素，每次 emit 前检查锁状态
     * 4. [collect] 取消或异常终止时释放租约
     *
     * 这样 [seal] 中的排干逻辑能正确等待正在收集的 Flow 完成。
     */
    fun <T> observeWithLease(block: suspend AppDatabase.() -> Flow<T>): Flow<T> = flow {
        acquireLease()
        try {
            val db = resolveDatabase()
            db.block().collect { value ->
                ensureUnlocked()
                emit(value)
            }
        } finally {
            releaseLease()
        }
    }.flowOn(Dispatchers.IO)

    // ============================== 锁管理 ==============================

    /**
     * 解锁。
     *
     * - 数据库已打开（SOFT_LOCKED → UNLOCKED）：仅改变状态
     * - 数据库未打开（SEALED → UNLOCKED）：使用 [dek] 打开数据库
     *
     * @return 打开数据库失败时返回异常，成功返回 null
     */
    suspend fun unlock(dek: ByteArray): Throwable? {
        if (database != null) {
            _lockState.value = LockState.UNLOCKED
            AppTelemetry.i(TAG, "Resumed from SOFT_LOCKED to UNLOCKED")
            return null
        }
        return openDatabase(dek)
    }

    /**
     * 软锁定：阻止新租约，数据库保持打开。
     * 适用于 UI 手动锁定或短暂遮挡认证页面。
     */
    suspend fun softLock() {
        _lockState.value = LockState.SOFT_LOCKED
        AppTelemetry.i(TAG, "Soft locked, database kept open")
    }

    /**
     * 封存：阻止新租约 → 排干活跃租约 → 关闭数据库。
     *
     * 排干超时时取消 [flowScope] 强制终止 Flow 观察，然后关闭数据库。
     * 适用于应用进后台、完整性异常、删除 Vault 等场景。
     */
    suspend fun seal(timeout: Duration = 5.seconds) {
        _lockState.value = LockState.SEALED

        val drained = withTimeoutOrNull(timeout) {
            while (activeLeases.get() > 0) {
                delay(DRAIN_POLL_MS)
            }
        }

        if (drained == null) {
            AppTelemetry.w(TAG, "Seal drain timeout, cancelling flow observations")
            flowScope.coroutineContext[Job]?.let { job ->
                job.children.forEach { it.cancel() }
            }
            // 被取消的 Flow 的 finally 块会释放租约，等待清理完成
            delay(POST_CANCEL_DELAY_MS)
            val remaining = activeLeases.get()
            if (remaining > 0) {
                AppTelemetry.e(TAG, "Force proceed with seal: $remaining leases may leak")
            }
        }

        closeDatabaseInternal()
        AppTelemetry.i(TAG, "Database sealed and closed")
    }

    /**
     * 仅关闭数据库，不改变锁状态。
     * 由 [DatabaseController] 调用。
     */
    suspend fun closeDatabase() {
        closeDatabaseInternal()
    }

    // ============================== 内部方法 ==============================

    /**
     * 获取租约。
     * 检查+递增在互斥锁下原子执行，避免状态变更与计数的竞态。
     */
    private suspend fun acquireLease() {
        leaseMutex.withLock {
            if (_lockState.value != LockState.UNLOCKED) {
                throw SessionLockedException("Session is ${_lockState.value.name}")
            }
            activeLeases.incrementAndGet()
        }
    }

    private fun releaseLease() {
        activeLeases.decrementAndGet()
    }

    private fun ensureUnlocked() {
        if (_lockState.value != LockState.UNLOCKED) {
            throw SessionLockedException("Session is ${_lockState.value.name}")
        }
    }

    private suspend fun resolveDatabase(): AppDatabase {
        dbMutex.withLock {
            database?.let { return it }
        }
        throw SessionLockedException("Database not opened — session is SEALED or never unlocked")
    }

    private suspend fun openDatabase(dek: ByteArray): Throwable? {
        return try {
            val db = databaseProvider.open(dek)
            dbMutex.withLock { database = db }
            _lockState.value = LockState.UNLOCKED
            AppTelemetry.i(TAG, "Database opened, state UNLOCKED")
            null
        } catch (e: Exception) {
            AppTelemetry.e(TAG, "Failed to open database", e)
            dbMutex.withLock { database = null }
            _lockState.value = LockState.SEALED
            e
        }
    }

    private suspend fun closeDatabaseInternal() {
        dbMutex.withLock {
            database?.let { db ->
                runCatching { db.close() }
                    .onFailure { e -> AppTelemetry.e(TAG, "Database close error", e) }
                database = null
                AppTelemetry.i(TAG, "Database connection closed")
            }
        }
    }
}
