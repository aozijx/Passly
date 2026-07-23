package com.aozijx.passly.data.local.dao.maintenance

import androidx.room.Dao
import androidx.room.Query

@Dao
interface VaultMaintenanceDao {

    @Query("DELETE FROM entry_search_tokens")
    suspend fun clearSearchTokens(): Int

    @Query("DELETE FROM entry_attachments")
    suspend fun clearAttachments(): Int

    @Query("DELETE FROM entry_activities")
    suspend fun clearActivities(): Int

    @Query("DELETE FROM entry_revisions")
    suspend fun clearRevisions(): Int

    @Query("DELETE FROM entry_secrets")
    suspend fun clearSecrets(): Int

    @Query("DELETE FROM vault_entries")
    suspend fun clearEntries(): Int

    @Query("DELETE FROM entry_drafts")
    suspend fun clearDrafts(): Int
}
