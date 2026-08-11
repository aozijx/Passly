package com.aozijx.passly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.error.boundary.DatabaseException
import com.aozijx.passly.data.local.dao.activity.EntryActivityAnalyticsDao
import com.aozijx.passly.data.local.dao.activity.EntryActivityCommandDao
import com.aozijx.passly.data.local.dao.activity.EntryActivityQueryDao
import com.aozijx.passly.data.local.dao.attachment.EntryAttachmentCommandDao
import com.aozijx.passly.data.local.dao.attachment.EntryAttachmentQueryDao
import com.aozijx.passly.data.local.dao.entry.EntryCommandDao
import com.aozijx.passly.data.local.dao.entry.EntryQueryDao
import com.aozijx.passly.data.local.dao.entry.EntrySecretCommandDao
import com.aozijx.passly.data.local.dao.entry.EntrySecretQueryDao
import com.aozijx.passly.data.local.dao.link.EntryLinkCommandDao
import com.aozijx.passly.data.local.dao.link.EntryLinkQueryDao
import com.aozijx.passly.data.local.dao.maintenance.VaultMaintenanceDao
import com.aozijx.passly.data.local.dao.revision.EntryRevisionCommandDao
import com.aozijx.passly.data.local.dao.revision.EntryRevisionQueryDao
import com.aozijx.passly.data.local.dao.search.SearchTokenCommandDao
import com.aozijx.passly.data.local.dao.search.SearchTokenQueryDao
import com.aozijx.passly.data.local.dao.sensitive.SensitiveFieldCommandDao
import com.aozijx.passly.data.local.dao.sensitive.SensitiveFieldQueryDao
import com.aozijx.passly.data.local.database.converter.ActivityTypeConverter
import com.aozijx.passly.data.local.database.converter.EntryRelationTypeConverter
import com.aozijx.passly.data.local.database.converter.EntryTypeConverter
import com.aozijx.passly.data.local.database.converter.LookupFieldConverter
import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity
import com.aozijx.passly.data.model.entity.EntryLinkEntity
import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.model.entity.EntryDraftEntity
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.model.entity.SearchTokenEntity

@Database(
    entities = [
        EntryEntity::class,
        EntrySecretEntity::class,
        EntrySensitiveFieldEntity::class,
        EntryLinkEntity::class,
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
    EntryRelationTypeConverter::class,
    ActivityTypeConverter::class,
    LookupFieldConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryQueryDao(): EntryQueryDao
    abstract fun entryCommandDao(): EntryCommandDao
    abstract fun entrySecretQueryDao(): EntrySecretQueryDao
    abstract fun entrySecretCommandDao(): EntrySecretCommandDao
    abstract fun sensitiveFieldQueryDao(): SensitiveFieldQueryDao
    abstract fun sensitiveFieldCommandDao(): SensitiveFieldCommandDao
    abstract fun entryLinkQueryDao(): EntryLinkQueryDao
    abstract fun entryLinkCommandDao(): EntryLinkCommandDao
    abstract fun entryRevisionQueryDao(): EntryRevisionQueryDao
    abstract fun entryRevisionCommandDao(): EntryRevisionCommandDao
    abstract fun entryActivityQueryDao(): EntryActivityQueryDao
    abstract fun entryActivityCommandDao(): EntryActivityCommandDao
    abstract fun entryActivityAnalyticsDao(): EntryActivityAnalyticsDao
    abstract fun entryAttachmentQueryDao(): EntryAttachmentQueryDao
    abstract fun entryAttachmentCommandDao(): EntryAttachmentCommandDao
    abstract fun searchTokenQueryDao(): SearchTokenQueryDao
    abstract fun searchTokenCommandDao(): SearchTokenCommandDao
    abstract fun vaultMaintenanceDao(): VaultMaintenanceDao

    companion object {
        private const val TAG = "AppDatabase"

        fun wrapError(e: Throwable): DatabaseException {
            return when {
                e.message?.contains("Migration", ignoreCase = true) == true ->
                    DatabaseException.MigrationFailedException(e)

                e.message?.contains("passphrase", ignoreCase = true) == true ->
                    DatabaseException.InvalidPassphraseException(e)

                else ->
                    DatabaseException.InitializationException("数据库初始化失败", e)
            }
        }
    }
}
