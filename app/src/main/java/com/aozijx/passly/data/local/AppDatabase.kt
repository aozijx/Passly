package com.aozijx.passly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.local.migration.Migrations
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [VaultEntryEntity::class, VaultHistoryEntity::class],
    version = DatabaseConfig.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun vaultHistoryDao(): VaultHistoryDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        var initializationError: Throwable? = null
            private set

        fun getDatabase(context: Context): AppDatabase {
            INSTANCE?.let { return it }

            return synchronized(this) {
                INSTANCE?.let { return@synchronized it }

                try {
                    val passphrase = DatabasePassphraseManager.getPassphrase()
                    val factory = SupportOpenHelperFactory(passphrase)

                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DatabaseConfig.DATABASE_NAME
                    )
                        .openHelperFactory(factory)
                        .addMigrations(*Migrations.getAll())
                        .build()

                    // 仅探测，不让探测的异常直接杀掉 getDatabase
                    runCatching { instance.openHelper.writableDatabase }
                        .onFailure { error ->
                            Logcat.e(TAG, "Database probe failed, recording error", error)
                            initializationError = wrapError(error)
                        }
                    
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Logcat.e(TAG, "Critical database setup failure", e)
                    val wrapped = wrapError(e)
                    initializationError = wrapped
                    throw wrapped
                }
            }
        }

        private fun wrapError(e: Throwable): DatabaseException {
            return when {
                e.message?.contains("Migration", ignoreCase = true) == true ->
                    DatabaseException.MigrationFailedException(e.message ?: "未知迁移错误", e)
                e.message?.contains("passphrase", ignoreCase = true) == true ->
                    DatabaseException.InvalidPassphraseException(e.message ?: "密钥错误")
                else ->
                    DatabaseException.InitializationException(e.message ?: "初始化失败", e)
            }
        }

        fun preWarm(context: Context) {
            getDatabase(context)
        }

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}