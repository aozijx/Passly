package com.aozijx.passly.security.crypto

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dekManager: DekManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "DbSessionMgr"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    @Volatile
    private var database: AppDatabase? = null

    override fun onStart(owner: LifecycleOwner) {
        observeLockState()
    }

    override fun onStop(owner: LifecycleOwner) {
        close()
    }

    private fun observeLockState() {
        dekManager.lockState
            .onEach { state ->
                when (state) {
                    LockState.LOCKED -> {
                        Logcat.i(TAG, "Lock state changed to LOCKED, closing database")
                        close()
                    }

                    LockState.UNLOCKED -> {
                        Logcat.i(TAG, "Lock state changed to UNLOCKED, database ready")
                    }
                }
            }
            .launchIn(scope)
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
            val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(dek.clone())
            androidx.room.Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                com.aozijx.passly.data.local.DatabaseConfig.DATABASE_NAME
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

    fun close() {
        scope.launch {
            mutex.withLock {
                database?.let {
                    it.close()
                    Logcat.i(TAG, "Database session closed")
                }
                database = null
            }
        }
    }

    suspend fun closeAndAwait() {
        mutex.withLock {
            database?.let {
                it.close()
                Logcat.i(TAG, "Database session closed")
            }
            database = null
        }
    }
}