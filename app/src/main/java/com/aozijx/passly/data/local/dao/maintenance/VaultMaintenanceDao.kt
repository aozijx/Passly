package com.aozijx.passly.data.local.dao.maintenance

import androidx.room.Dao
import androidx.room.Query

@Dao
interface VaultMaintenanceDao {

    @Query("DELETE FROM entry_search_tokens")
    suspend fun clearSearchTokens(): Int

    @Query("DELETE FROM entry_attachments WHERE status = 'COMMITTED'")
    suspend fun clearAttachments(): Int

    /** 清理所有 PENDING 附件（崩溃恢复时调用） */
    @Query("DELETE FROM entry_attachments WHERE status = 'PENDING'")
    suspend fun clearPending(): Int

    @Query("DELETE FROM entry_activities")
    suspend fun clearActivities(): Int

    @Query("DELETE FROM entry_revisions")
    suspend fun clearRevisions(): Int

    @Query("DELETE FROM entry_secrets")
    suspend fun clearSecrets(): Int

    @Query("DELETE FROM entries")
    suspend fun clearEntries(): Int

    @Query("DELETE FROM entry_drafts")
    suspend fun clearDrafts(): Int
}
