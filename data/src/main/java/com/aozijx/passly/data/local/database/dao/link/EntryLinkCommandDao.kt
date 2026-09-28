package com.aozijx.passly.data.local.database.dao.link

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity

@Dao
interface EntryLinkCommandDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(links: List<EntryLinkEntity>)

    @Upsert
    suspend fun upsert(link: EntryLinkEntity)

    @Query("DELETE FROM entry_links WHERE linkId = :linkId")
    suspend fun deleteById(linkId: String): Int

    @Query("DELETE FROM entry_links WHERE sourceEntryId = :entryId OR targetEntryId = :entryId")
    suspend fun deleteByEntryId(entryId: String): Int
}
