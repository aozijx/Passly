package com.aozijx.passly.data.local.database.dao.activity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity

@Dao
internal interface EntryActivityCommandDao {
    // === Strict Insert (ignore duplicate) ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIdempotent(activity: EntryActivityEntity)

    // === Maintenance API ===

    @Query("DELETE FROM entry_activities WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_activities WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)
}
