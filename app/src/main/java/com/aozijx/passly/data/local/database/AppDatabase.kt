package com.aozijx.passly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.error.database.DatabaseException
import com.aozijx.passly.data.local.dao.EntryActivityDao
import com.aozijx.passly.data.local.dao.EntryAttachmentDao
import com.aozijx.passly.data.local.dao.EntryDao
import com.aozijx.passly.data.local.dao.EntryDraftDao
import com.aozijx.passly.data.local.dao.EntryRevisionDao
import com.aozijx.passly.data.local.dao.EntrySecretDao
import com.aozijx.passly.data.local.dao.SearchTokenDao
import com.aozijx.passly.data.local.database.converter.ActivityTypeConverter
import com.aozijx.passly.data.local.database.converter.EntryTypeConverter
import com.aozijx.passly.data.local.database.converter.LookupFieldConverter
import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity
import com.aozijx.passly.data.model.entity.EntryDraftEntity
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.model.entity.SearchTokenEntity

@Database(
    entities = [
        EntryEntity::class,
        EntrySecretEntity::class,
        EntryRevisionEntity::class,
        EntryActivityEntity::class,
        EntryAttachmentEntity::class,
        SearchTokenEntity::class,
        EntryDraftEntity::class
    ],
    version = DatabaseSchema.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
@TypeConverters(
    EntryTypeConverter::class,
    ActivityTypeConverter::class,
    LookupFieldConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun entrySecretDao(): EntrySecretDao
    abstract fun entryRevisionDao(): EntryRevisionDao
    abstract fun entryActivityDao(): EntryActivityDao
    abstract fun entryAttachmentDao(): EntryAttachmentDao
    abstract fun searchTokenDao(): SearchTokenDao
    abstract fun entryDraftDao(): EntryDraftDao

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
