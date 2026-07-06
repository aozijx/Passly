package com.aozijx.passly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao

@Database(
    entities = [VaultEntryEntity::class, VaultHistoryEntity::class],
    version = DatabaseConfig.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun vaultHistoryDao(): VaultHistoryDao

    companion object {
        private const val TAG = "AppDatabase"

        fun wrapError(e: Throwable): DatabaseException {
            return when {
                e.message?.contains("Migration", ignoreCase = true) == true ->
                    DatabaseException.MigrationFailedException(e.message ?: "未知迁移错误", e)
                e.message?.contains("passphrase", ignoreCase = true) == true ->
                    DatabaseException.InvalidPassphraseException(e.message ?: "密钥错误")
                else ->
                    DatabaseException.InitializationException(e.message ?: "初始化失败", e)
            }
        }
    }
}
