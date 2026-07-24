package com.aozijx.passly.core.session

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.DatabaseInitFailed
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 统一会话管理器。
 *
 * 作为 [DatabaseLeaseGate] 的外层协调者，负责：
 * - 从 [DekManager] 获取 DEK 传递给 [DatabaseLeaseGate]
 * - 提供向后兼容的 [query]/[transaction]/[observeFlow] API
 * - 实现 [SessionStateProvider] 接口
 *
 * ## 锁策略
 * - [softLock]：阻止新租约，数据库保持打开（SOFT_LOCKED）。
 *   适用于 UI 手动锁定或空闲超时。
 * - [seal]：排干租约、关闭数据库（SEALED）。
 *   适用于应用进后台、完整性异常、删除 Vault。
 * - [unlock]：打开数据库或恢复解锁状态。
 *
 * DEK 擦除由 [DekManager] 负责，本类不触及 DEK 生命周期。
 */
@Singleton
class UnifiedSessionManager @Inject constructor(
    private val leaseGate: DatabaseLeaseGate,
    private val dekManager: DekManager
) : SessionStateProvider {

    private companion object {
        private const val TAG = "UnifiedSessionManager"
    }

    // ============================== SessionStateProvider ==============================

    override val lockState: LockState
        get() = leaseGate.lockState.value

    // ============================== 公共 API ==============================

    /**
     * 即查即返（一次性挂起查询）。
     * 委派给 [DatabaseLeaseGate.withReadLease]。
     */
    suspend fun <T> query(block: suspend AppDatabase.() -> T): T =
        leaseGate.withReadLease {
            block()
        }

    /**
     * 写事务。
     * 委派给 [DatabaseLeaseGate.withWriteLease]。
     */
    suspend fun <T> transaction(block: suspend AppDatabase.() -> T): T =
        leaseGate.withWriteLease {
            block()
        }

    /**
     * 持续观察 Flow。
     * 委派给 [DatabaseLeaseGate.observeWithLease]。
     */
    fun <T> observeFlow(block: suspend AppDatabase.() -> Flow<T>): Flow<T> =
        leaseGate.observeWithLease {
            block()
        }

    // ============================== 锁管理 ==============================

    /**
     * 解锁会话。
     *
     * - 数据库已打开（SOFT_LOCKED → UNLOCKED）：仅恢复状态
     * - 数据库未打开（SEALED → UNLOCKED）：获取 DEK 打开数据库
     *
     * @return 打开数据库失败时返回 [DatabaseInitFailed]，成功返回 null
     */
    suspend fun unlock(): Throwable? {
        val currentLock = leaseGate.lockState.value
        // SOFT_LOCKED → UNLOCKED: 数据库已打开，仅恢复状态
        if (currentLock == LockState.SOFT_LOCKED) {
            return leaseGate.unlock(ByteArray(0)) // DEK 不被使用，传空数组
        }
        // SEALED → UNLOCKED: 需要 DEK 打开数据库
        return withContext(Dispatchers.IO) {
            val dek = try {
                dekManager.withDek { it.clone() }
            } catch (e: IllegalStateException) {
                AppLog.e(TAG, "DEK not available for unlock", e)
                return@withContext DatabaseInitFailed("DEK not available, re-authentication required")
            }
            try {
                val err = leaseGate.unlock(dek)
                if (err != null) {
                    AppLog.e(TAG, "Failed to open database on unlock", err)
                }
                err
            } finally {
                dek.fill(0)
            }
        }
    }

    /**
     * 软锁定：阻止新租约，数据库保持打开。
     * 适用于 UI 手动锁定或空闲超时。
     */
    suspend fun softLock() {
        AppLog.i(TAG, "Soft locking session")
        leaseGate.softLock()
    }

    /**
     * 封存：排干租约 → 关闭数据库。
     * 适用于应用进后台、完整性异常、删除 Vault。
     * 调用方应在此后擦除 DEK。
     */
    suspend fun seal(timeout: Duration = 5.seconds) {
        AppLog.i(TAG, "Sealing session")
        leaseGate.seal(timeout)
    }

    /**
     * 关闭数据库连接（旧 API）。
     * 仅由 [DatabaseController] 在预热/重试时调用。
     */
    suspend fun closeDatabase() {
        leaseGate.closeDatabase()
    }
}
