package com.aozijx.passly.data.local.dao.search

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.domain.model.lookup.LookupField

@Dao
interface SearchTokenQueryDao {

    @Query("SELECT entryId FROM entry_search_tokens WHERE keywordHash = :hash AND gramLength = :length AND field IN (:fields) ORDER BY weight DESC")
    suspend fun searchByHash(hash: ByteArray, length: Int, fields: List<LookupField>): List<String>

    @RawQuery(observedEntities = [SearchTokenEntity::class])
    suspend fun searchByTokenIntersection(query: SupportSQLiteQuery): List<String>

    @Query("SELECT * FROM entry_search_tokens WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<SearchTokenEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM entry_search_tokens WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    @Query("SELECT COUNT(*) FROM entry_search_tokens")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT entryId) FROM entry_search_tokens")
    suspend fun countDistinctEntryIds(): Int
}
