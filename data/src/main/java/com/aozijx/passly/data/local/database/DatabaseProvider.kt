package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import com.aozijx.passly.data.local.database.callback.AttachmentReferenceConstraintCallback
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
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
    @param:ApplicationContext private val context: Context,
    private val logger: Logger,
) {
    /**
     * 打开数据库。
     *
     * @param key SQLCipher 加密密钥（由调用方从 DEK 派生，调用完毕后应擦除）
     */
    suspend fun open(key: ByteArray): AppDatabase = open(DatabaseSchema.DATABASE_NAME, key)

    internal suspend fun open(databaseName: String, key: ByteArray): AppDatabase =
        withContext(Dispatchers.IO) {
            logger.inSpan(Operations.DatabaseOpen) {
            require(databaseName.matches(Regex("[a-zA-Z0-9_.-]+"))) { "Invalid database name" }
            val factory = SupportOpenHelperFactory(key.copyOf())
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .addCallback(AttachmentReferenceConstraintCallback)
                .build()

            runCatching { db.openHelper.writableDatabase }
                .onFailure { error ->
                    logger.log(LogStatements.Database.openFailed(error))
                    db.close()
                    throw error
                }

            logger.log(LogStatements.Database.opened())
            db
            }
        }
}
