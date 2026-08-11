package com.aozijx.passly.data.local.dao.link

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryLinkQueryDao {
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
