package com.aozijx.passly.data.local.database.dao.maintenance

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DatabaseMaintenanceDao {

    @Query("DELETE FROM attachment_refs")
    suspend fun clearAttachments(): Int

    @Query("DELETE FROM entry_activities")
    suspend fun clearActivities(): Int

    @Query("DELETE FROM entry_revisions")
    suspend fun clearRevisions(): Int

    @Query("DELETE FROM attachment_resources")
    suspend fun clearAttachmentResources(): Int

    @Query("DELETE FROM attachment_resources WHERE lifecycleState = 'PENDING_GC'")
    suspend fun clearAttachmentGcQueue(): Int

    @Query("DELETE FROM entry_secret_fields")
    suspend fun clearSecrets(): Int

    @Query("DELETE FROM entries")
    suspend fun clearEntries(): Int

}
