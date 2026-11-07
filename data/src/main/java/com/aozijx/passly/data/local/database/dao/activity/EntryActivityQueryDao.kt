package com.aozijx.passly.data.local.database.dao.activity

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryActivityQueryDao {
    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY createdAt DESC, activityId DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY createdAt DESC, activityId DESC")
    suspend fun getByEntryId(entryId: String): List<EntryActivityEntity>

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId AND activityType = :activityType")
    suspend fun countByEntryIdAndType(
        entryId: String,
        activityType: ActivityType,
    ): Int
}
