package com.aozijx.passly.data.local.database.dao.entry

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntrySecretEntity

@Dao
interface EntrySecretQueryDao {

    // ---- get (suspend) ----

    @Query("SELECT * FROM entry_secrets WHERE entryId = :entryId LIMIT 1")
    suspend fun getByEntryId(entryId: String): EntrySecretEntity?

    @Query("SELECT * FROM entry_secrets WHERE entryId IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<String>): List<EntrySecretEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entry_secrets WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM entry_secrets")
    suspend fun count(): Int
}
