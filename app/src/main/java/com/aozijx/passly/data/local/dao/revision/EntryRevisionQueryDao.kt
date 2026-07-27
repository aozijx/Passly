package com.aozijx.passly.data.local.dao.revision

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRevisionQueryDao {

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryRevisionEntity>>

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId AND version = :version")
    suspend fun getByVersion(entryId: String, version: Int): EntryRevisionEntity?
}
