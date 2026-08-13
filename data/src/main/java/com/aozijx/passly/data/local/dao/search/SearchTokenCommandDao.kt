package com.aozijx.passly.data.local.dao.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.domain.entry.model.lookup.LookupField

@Dao
interface SearchTokenCommandDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entity: SearchTokenEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(entities: List<SearchTokenEntity>)

    @Upsert
    suspend fun upsertForImport(entity: SearchTokenEntity)

    @Upsert
    suspend fun upsertAllForImport(entities: List<SearchTokenEntity>)

    @Query("DELETE FROM entry_search_tokens WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_search_tokens WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM entry_search_tokens WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: LookupField)
}
