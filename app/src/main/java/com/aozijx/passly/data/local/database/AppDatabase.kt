package com.aozijx.passly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.data.local.DatabaseException
import com.aozijx.passly.data.local.dao.KeyEnvelopeDao
import com.aozijx.passly.data.local.dao.LookupIndexDao
import com.aozijx.passly.data.local.dao.VaultActivityDao
import com.aozijx.passly.data.local.dao.VaultAttachmentDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.local.dao.VaultMetadataDao
import com.aozijx.passly.data.local.dao.VaultPayloadDao
import com.aozijx.passly.data.model.entity.KeyEnvelopeEntity
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.data.model.entity.VaultActivityEntity
import com.aozijx.passly.data.model.entity.VaultAttachmentEntity
import com.aozijx.passly.data.model.entity.VaultHistoryEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.entity.VaultPayloadEntity

@Database(
    entities = [
        VaultMetadataEntity::class,
        VaultPayloadEntity::class,
        VaultHistoryEntity::class,
        VaultActivityEntity::class,
        VaultAttachmentEntity::class,
        LookupIndexEntity::class,
        KeyEnvelopeEntity::class
    ],
    version = DatabaseConfig.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun metadataDao(): VaultMetadataDao
    abstract fun payloadDao(): VaultPayloadDao
    abstract fun historyDao(): VaultHistoryDao
    abstract fun activityDao(): VaultActivityDao
    abstract fun attachmentDao(): VaultAttachmentDao
    abstract fun lookupIndexDao(): LookupIndexDao
    abstract fun keyEnvelopeDao(): KeyEnvelopeDao

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