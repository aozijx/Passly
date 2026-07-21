package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import com.aozijx.passly.core.diagnostics.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库资源层。
 *
 * 只负责：key → SupportOpenHelperFactory → Room.databaseBuilder() → AppDatabase
 * 不知道：Auth 状态、Session 生命周期、DEK 来源
 */
@Singleton
class DatabaseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseProvider"
    }

    /**
     * 打开数据库。
     *
     * @param key SQLCipher 加密密钥（由调用方从 DEK 派生，调用完毕后应擦除）
     */
    suspend fun open(key: ByteArray): AppDatabase = withContext(Dispatchers.IO) {
        val factory = SupportOpenHelperFactory(key.copyOf())
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DatabaseSchema.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .build()

        // 这里现在安全了
        runCatching { db.openHelper.writableDatabase }
            .onFailure { error ->
                AppLog.e(TAG, "Database probe failed", error)
                db.close()
                throw error
            }

        AppLog.i(TAG, "Database opened successfully")
        db
    }
}
