package com.aozijx.passly.data.local.database.dao.link

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryLinkQueryDao {
    @Query("SELECT * FROM entry_links ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<EntryLinkEntity>>

    @Query("SELECT * FROM entry_links ORDER BY createdAt ASC")
    suspend fun getAll(): List<EntryLinkEntity>

    @Query("SELECT * FROM entry_links WHERE linkId = :linkId LIMIT 1")
    suspend fun getById(linkId: String): EntryLinkEntity?

    @Query(
        """
        SELECT * FROM entry_links
        WHERE sourceEntryId = :entryId OR targetEntryId = :entryId
        ORDER BY createdAt ASC
        """
    )
    fun observeByEntryId(entryId: String): Flow<List<EntryLinkEntity>>

    @Query(
        """
        SELECT * FROM entry_links
        WHERE sourceEntryId = :entryId OR targetEntryId = :entryId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getByEntryId(entryId: String): List<EntryLinkEntity>
}
