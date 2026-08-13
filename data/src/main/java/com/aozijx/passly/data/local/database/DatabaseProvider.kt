package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import com.aozijx.passly.data.local.database.callback.AttachmentReferenceConstraintCallback
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryReporter
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
    private val telemetry: TelemetryReporter,
) {
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
            .addCallback(AttachmentReferenceConstraintCallback)
            .build()

        runCatching { db.openHelper.writableDatabase }
            .onFailure { error ->
                telemetry.emit(
                    TelemetryEvent(
                        level = EventLevel.ERROR,
                        category = EventCategory.DATABASE,
                        name = "database.open_failed",
                        throwableType = error.javaClass.simpleName,
                    )
                )
                db.close()
                throw error
            }

        telemetry.emit(
            TelemetryEvent(
                level = EventLevel.INFO,
                category = EventCategory.DATABASE,
                name = "database.opened",
            )
        )
        db
    }
}
