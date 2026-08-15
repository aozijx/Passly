package com.aozijx.passly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aozijx.passly.data.BuildConfig
import com.aozijx.passly.data.local.database.converter.ActivityTypeConverter
import com.aozijx.passly.data.local.database.converter.EntryRelationTypeConverter
import com.aozijx.passly.data.local.database.converter.EntryTypeConverter
import com.aozijx.passly.data.local.database.converter.LookupFieldConverter
import com.aozijx.passly.data.local.database.dao.activity.EntryActivityAnalyticsDao
import com.aozijx.passly.data.local.database.dao.activity.EntryActivityCommandDao
import com.aozijx.passly.data.local.database.dao.activity.EntryActivityQueryDao
import com.aozijx.passly.data.local.database.dao.attachment.AttachmentGcQueueDao
import com.aozijx.passly.data.local.database.dao.attachment.AttachmentRefCommandDao
import com.aozijx.passly.data.local.database.dao.attachment.AttachmentRefQueryDao
import com.aozijx.passly.data.local.database.dao.attachment.AttachmentResourceDao
import com.aozijx.passly.data.local.database.dao.attachment.RevisionAttachmentRefDao
import com.aozijx.passly.data.local.database.dao.entry.EntryCommandDao
import com.aozijx.passly.data.local.database.dao.entry.EntryQueryDao
import com.aozijx.passly.data.local.database.dao.link.EntryLinkCommandDao
import com.aozijx.passly.data.local.database.dao.link.EntryLinkQueryDao
import com.aozijx.passly.data.local.database.dao.maintenance.DatabaseMaintenanceDao
import com.aozijx.passly.data.local.database.dao.revision.EntryRevisionCommandDao
import com.aozijx.passly.data.local.database.dao.revision.EntryRevisionQueryDao
import com.aozijx.passly.data.local.database.dao.search.SearchTokenCommandDao
import com.aozijx.passly.data.local.database.dao.search.SearchTokenQueryDao
import com.aozijx.passly.data.local.database.dao.secret.SecretFieldCommandDao
import com.aozijx.passly.data.local.database.dao.secret.SecretFieldQueryDao
import com.aozijx.passly.data.local.database.entity.AttachmentGcQueueEntity
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.SearchTokenEntity

@Database(
    entities = [
        EntryEntity::class,
        EntrySecretFieldEntity::class,
        EntryLinkEntity::class,
        EntryRevisionEntity::class,
        EntryActivityEntity::class,
        AttachmentResourceEntity::class,
        AttachmentRefEntity::class,
        RevisionAttachmentRefEntity::class,
        AttachmentGcQueueEntity::class,
        SearchTokenEntity::class
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
    abstract fun secretFieldQueryDao(): SecretFieldQueryDao
    abstract fun secretFieldCommandDao(): SecretFieldCommandDao
    abstract fun entryLinkQueryDao(): EntryLinkQueryDao
    abstract fun entryLinkCommandDao(): EntryLinkCommandDao
    abstract fun entryRevisionQueryDao(): EntryRevisionQueryDao
    abstract fun entryRevisionCommandDao(): EntryRevisionCommandDao
    abstract fun entryActivityQueryDao(): EntryActivityQueryDao
    abstract fun entryActivityCommandDao(): EntryActivityCommandDao
    abstract fun entryActivityAnalyticsDao(): EntryActivityAnalyticsDao
    abstract fun attachmentRefQueryDao(): AttachmentRefQueryDao
    abstract fun attachmentRefCommandDao(): AttachmentRefCommandDao
    abstract fun attachmentResourceDao(): AttachmentResourceDao
    abstract fun attachmentGcQueueDao(): AttachmentGcQueueDao
    abstract fun revisionAttachmentRefDao(): RevisionAttachmentRefDao
    abstract fun searchTokenQueryDao(): SearchTokenQueryDao
    abstract fun searchTokenCommandDao(): SearchTokenCommandDao
    abstract fun databaseMaintenanceDao(): DatabaseMaintenanceDao
}
