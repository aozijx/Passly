package com.aozijx.passly.data.local.database.dao.maintenance

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DatabaseMaintenanceDao {

    @Query("DELETE FROM entry_search_tokens")
    suspend fun clearSearchTokens(): Int

    @Query("DELETE FROM attachment_refs WHERE status = 'COMMITTED'")
    suspend fun clearAttachments(): Int

    /** 清理所有 PENDING 附件（崩溃恢复时调用） */
    @Query("DELETE FROM attachment_refs WHERE status = 'PENDING'")
    suspend fun clearPending(): Int

    @Query("DELETE FROM entry_activities")
    suspend fun clearActivities(): Int

    @Query("DELETE FROM entry_revisions")
    suspend fun clearRevisions(): Int

    @Query("DELETE FROM attachment_resources")
    suspend fun clearAttachmentResources(): Int

    @Query("DELETE FROM attachment_gc_queue")
    suspend fun clearAttachmentGcQueue(): Int

    @Query("DELETE FROM entry_secret_fields")
    suspend fun clearSecrets(): Int

    @Query("DELETE FROM entries")
    suspend fun clearEntries(): Int

}
