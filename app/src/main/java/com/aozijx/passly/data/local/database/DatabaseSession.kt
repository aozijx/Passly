package com.aozijx.passly.data.local.database

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.authentication.VaultResourceController
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * 只负责：App Start → DatabaseProvider.open() → 保存 Database → withDatabase() → Vault Lock → close()
 * 不知道：DEK、SupportOpenHelperFactory、Room.databaseBuilder
 */
@Singleton
class DatabaseSession @Inject constructor(
    private val provider: DatabaseProvider,
    private val dekManager: DekManager
) : DefaultLifecycleObserver, VaultResourceController {

    companion object {
        private const val TAG = "DatabaseSession"
        private const val CLOSE_TIMEOUT_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val leaseMutex = Mutex()

    private var acceptsLeases = true
    private var activeLeases = 0
    private var leasesDrained = CompletableDeferred(Unit)

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
                var db = database
                if (db == null) {
                    mutex.withLock {
                        db = database
                        if (db == null) {
                            db = provider.open()
                            database = db
                        }
                    }
                }
                block(checkNotNull(db))
            }
        } finally {
            releaseLease()
        }
    }

    override suspend fun blockNewAccess() {
        val drained = leaseMutex.withLock {
            acceptsLeases = false
            if (activeLeases == 0) null else leasesDrained
        }
        drained?.await()
    }

    override suspend fun allowAccess() = leaseMutex.withLock { acceptsLeases = true }

    /**
     * 关闭数据库会话并等待完成。
     *
     * 使用超时保护（5秒），防止数据库因死锁等原因长时间无法关闭。
     * 超时后强制将 database 置为 null，可能存在资源泄漏但总比永久阻塞好。
     */
    override suspend fun closeAndAwait() {
        if (database == null) return

        val closed = withTimeoutOrNull(CLOSE_TIMEOUT_MS) {
            mutex.withLock {
                database?.let {
                    runCatching { it.close() }
                        .onFailure { e -> Logcat.e(TAG, "Database close error", e) }
                    Logcat.i(TAG, "Database session closed")
                }
                database = null
            }
        }

        if (closed == null) {
            Logcat.e(TAG, "Database close timed out after $CLOSE_TIMEOUT_MS ms, forcing null")
            database = null
        }
    }

    private suspend fun acquireLease() = leaseMutex.withLock {
        check(acceptsLeases) { "Vault database is locked" }
        if (activeLeases == 0) leasesDrained = CompletableDeferred()
        activeLeases += 1
    }

    private suspend fun releaseLease() = leaseMutex.withLock {
        activeLeases -= 1
        if (activeLeases == 0) leasesDrained.complete(Unit)
    }
}
