package com.aozijx.passly.data.local.database.dao.link

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity

@Dao
interface EntryLinkCommandDao {
    @Upsert
    suspend fun upsert(link: EntryLinkEntity)

    @Query("DELETE FROM entry_links WHERE linkId = :linkId")
    suspend fun deleteById(linkId: String): Int
}
