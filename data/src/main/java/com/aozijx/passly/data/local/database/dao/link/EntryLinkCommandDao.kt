package com.aozijx.passly.data.local.database.dao.link

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity

@Dao
interface EntryLinkCommandDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(links: List<EntryLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(link: EntryLinkEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateExisting(link: EntryLinkEntity): Int

    // Room @Upsert can swallow a secondary-unique conflict when its PK update affects zero rows.
    @Transaction
    suspend fun upsert(link: EntryLinkEntity) {
        if (updateExisting(link) == 0) insertStrict(link)
    }

    @Query("DELETE FROM entry_links WHERE linkId = :linkId")
    suspend fun deleteById(linkId: String): Int

    @Query("DELETE FROM entry_links WHERE sourceEntryId = :entryId OR targetEntryId = :entryId")
    suspend fun deleteByEntryId(entryId: String): Int
}
