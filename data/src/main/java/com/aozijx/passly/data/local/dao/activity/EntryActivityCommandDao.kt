package com.aozijx.passly.data.local.dao.activity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.EntryActivityEntity

@Dao
interface EntryActivityCommandDao {

    // === Strict Insert (ignore duplicate) ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIdempotent(activity: EntryActivityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIdempotent(activities: List<EntryActivityEntity>)

    // === Import Upsert ===

    @Upsert
    suspend fun upsertForImport(activity: EntryActivityEntity)

    @Upsert
    suspend fun upsertAllForImport(activities: List<EntryActivityEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM entry_activities WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_activities WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM entry_activities WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)
}
