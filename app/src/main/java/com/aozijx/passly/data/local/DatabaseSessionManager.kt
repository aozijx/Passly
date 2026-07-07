package com.aozijx.passly.data.local

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.room.Room
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.security.crypto.DekManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dekManager: DekManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "DbSessionMgr"
        private const val CLOSE_TIMEOUT_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

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

    suspend fun <T> withDatabase(block: suspend AppDatabase.() -> T): T =
        withContext(Dispatchers.IO) {
            var db = database
            if (db == null) {
                mutex.withLock {
                    db = database
                    if (db == null) {
                        db = createDatabase()
                    }
                }
            }
            block(checkNotNull(db))
        }

    private suspend fun createDatabase(): AppDatabase {
        val db = dekManager.withDek { dek ->
            val factory = SupportOpenHelperFactory(dek.clone())
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DatabaseConfig.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()
        }

        runCatching { db.openHelper.writableDatabase }
            .onFailure { error ->
                Logcat.e(TAG, "Database probe failed", error)
                db.close()
                throw error
            }

        database = db
        Logcat.i(TAG, "Database session created")
        return db
    }

    /**
     * 关闭数据库会话并等待完成。
     *
     * 使用超时保护（5秒），防止数据库因死锁等原因长时间无法关闭。
     * 超时后强制将 database 置为 null，可能存在资源泄漏但总比永久阻塞好。
     */
    suspend fun closeAndAwait() {
        // 快速路径：如果已经为空，直接返回
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
            // 超时后强制置空，可能存在资源泄漏，但总比永久阻塞好
            database = null
        }
    }
}