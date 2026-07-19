package com.aozijx.passly.data.local.database

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.authentication.VaultResourceController
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责数据库会话管理。
 *
 * 实现了租约机制 (Lease mechanism)：
 * 1. 访问数据库前必须获取租约。
 * 2. 如果数据库处于锁定状态，获取租约的操作会挂起等待解锁，而不是直接崩溃。
 */
@Singleton
class DatabaseSession @Inject constructor(
    private val provider: DatabaseProvider,
    private val dekManager: DekManager
) : DefaultLifecycleObserver, VaultResourceController {

    companion object {
        private const val TAG = "DatabaseSession"
        private const val CLOSE_TIMEOUT_MS = 5000L
        private const val ACQUIRE_LEASE_TIMEOUT_MS = 8000L // 等待解锁的最长时间
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val leaseMutex = Mutex()

    // 使用 StateFlow 追踪准入状态，支持异步挂起等待
    private val acceptsLeases = MutableStateFlow(false)
    private var activeLeases = 0
    private var leasesDrained = CompletableDeferred<Unit>().apply { complete(Unit) }

    @Volatile
    private var database: AppDatabase? = null

    override fun onStart(owner: LifecycleOwner) {
        dekManager.setLockCallback { closeAndAwait() }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            closeAndAwait()
        }
    }

    suspend fun <T> withDatabase(block: suspend AppDatabase.() -> T): T {
        acquireLease()
        return try {
            withContext(Dispatchers.IO) {
                val db = getOrOpenDatabase()
                block(db)
            }
        } finally {
            releaseLease()
        }
    }

    private suspend fun getOrOpenDatabase(): AppDatabase {
        // 快速路径：如果已存在则直接返回
        database?.let { return it }

        return mutex.withLock {
            // 双重检查
            database?.let { return it }

            // 在打开前再次确认当前是否允许访问（防止在 lock 到一半时 unlock 导致的竞争）
            leaseMutex.withLock {
                check(acceptsLeases.value) { "Vault database is locked, cannot open session" }
            }

            AppLog.i(TAG, "Opening new database session")
            val db = provider.open()
            database = db
            db
        }
    }

    override suspend fun blockNewAccess() {
        val drained = leaseMutex.withLock {
            acceptsLeases.value = false
            if (activeLeases == 0) null else leasesDrained
        }
        drained?.await()
        AppLog.d(TAG, "Database access blocked and leases drained")
    }

    override suspend fun allowAccess() {
        leaseMutex.withLock {
            acceptsLeases.value = true
        }
        AppLog.d(TAG, "Database access allowed")
    }

    override suspend fun closeAndAwait() {
        // 首先确保状态标记为不可访问，并等待所有并发操作完成
        blockNewAccess()

        val closed = withTimeoutOrNull(CLOSE_TIMEOUT_MS) {
            mutex.withLock {
                database?.let { db ->
                    runCatching { db.close() }
                        .onFailure { e -> AppLog.e(TAG, "Database close error", e) }
                    AppLog.i(TAG, "Database session closed")
                }
                database = null
            }
        }

        if (closed == null) {
            AppLog.e(TAG, "Database close timed out, forcing reference cleanup")
            // 即使超时也要确保引用被清除，防止旧句柄被后续操作错误复用
            mutex.withLock { database = null }
        }
    }

    /**
     * 获取租约。
     * 如果 acceptsLeases 为 false，则会挂起直到它变为 true。
     */
    private suspend fun acquireLease() {
        // 关键逻辑：如果当前被锁定，则挂起等待 acceptsLeases 变为 true
        val isUnlocked = withTimeoutOrNull(ACQUIRE_LEASE_TIMEOUT_MS) {
            acceptsLeases.first { it }
        }

        if (isUnlocked != true) {
            throw IllegalStateException("Vault database is locked (timeout waiting for unlock)")
        }

        leaseMutex.withLock {
            // 二次确认，防止在等待结束到进入锁的瞬间状态又变了
            if (!acceptsLeases.value) {
                throw IllegalStateException("Vault database is locked")
            }

            if (activeLeases == 0) {
                leasesDrained = CompletableDeferred()
            }
            activeLeases += 1
        }
    }

    private suspend fun releaseLease() = leaseMutex.withLock {
        activeLeases -= 1
        if (activeLeases == 0) {
            leasesDrained.complete(Unit)
        }
    }
}
