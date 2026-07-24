package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDraftDao {

    @Query("SELECT * FROM entry_drafts WHERE entryId = :entryId ORDER BY updatedAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryDraftEntity>>

    @Query("SELECT * FROM entry_drafts WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<EntryDraftEntity>>

    @Query("SELECT * FROM entry_drafts WHERE draftId = :draftId LIMIT 1")
    suspend fun getById(draftId: String): EntryDraftEntity?

    @Query("SELECT * FROM entry_drafts WHERE entryId = :entryId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestByEntryId(entryId: String): EntryDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: EntryDraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(drafts: List<EntryDraftEntity>)

    @Query("DELETE FROM entry_drafts WHERE draftId = :draftId")
    suspend fun deleteById(draftId: String)

    @Query("DELETE FROM entry_drafts WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_drafts WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

}
