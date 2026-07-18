package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.security.crypto.DekManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责创建数据库。
 *
 * 只负责：DEK → SupportOpenHelperFactory → Room.databaseBuilder() → AppDatabase
 * 不知道：生命周期、onStop、close、Session
 */
@Singleton
class DatabaseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dekManager: DekManager
) {
    companion object {
        private const val TAG = "DatabaseProvider"
    }

    /**
     * 打开数据库。每次调用都会通过 [DekManager.withDek] 获取 DEK，
     * 创建新的 [SupportOpenHelperFactory] 和 Room 数据库实例。
     */
    suspend fun open(): AppDatabase {
        val db = dekManager.withDek { dek ->
            val factory = SupportOpenHelperFactory(dek.clone())
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DatabaseSchema.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()
        }

        runCatching { db.openHelper.writableDatabase }
            .onFailure { error ->
                AppLog.e(TAG, "Database probe failed", error)
                db.close()
                throw error
            }

        AppLog.i(TAG, "Database created")
        return db
    }
}
