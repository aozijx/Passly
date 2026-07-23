package com.aozijx.passly.data.local.dao.revision

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.EntryRevisionEntity

@Dao
interface EntryRevisionCommandDao {

    // === Idempotent Insert (ignore duplicate) ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIdempotent(revision: EntryRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIdempotent(revisions: List<EntryRevisionEntity>)

    // === Import Upsert (overwrite on duplicate) ===

    @Upsert
    suspend fun upsertForImport(revision: EntryRevisionEntity)

    @Upsert
    suspend fun upsertAllForImport(revisions: List<EntryRevisionEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId AND version <= (SELECT version FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC LIMIT 1 OFFSET :keepCount)")
    suspend fun deleteOldVersions(entryId: String, keepCount: Int)

    @Query("DELETE FROM entry_revisions WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)
}
