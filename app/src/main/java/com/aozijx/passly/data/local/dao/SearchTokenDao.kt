package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.domain.model.lookup.LookupField

@Dao
interface SearchTokenDao {

    // ---- search (single token) ----

    @Query("SELECT entryId FROM entry_search_tokens WHERE keywordHash = :hash AND gramLength = :length AND field IN (:fields) ORDER BY weight DESC")
    suspend fun searchByHash(hash: ByteArray, length: Int, fields: List<LookupField>): List<String>

    @RawQuery(observedEntities = [SearchTokenEntity::class])
    suspend fun searchByTokenIntersection(query: SupportSQLiteQuery): List<String>

    // ---- get (suspend) ----

    @Query("SELECT * FROM entry_search_tokens WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<SearchTokenEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entry_search_tokens WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count / status ----

    @Query("SELECT COUNT(*) FROM entry_search_tokens")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT entryId) FROM entry_search_tokens")
    suspend fun countDistinctEntryIds(): Int

    // === Strict Insert ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entity: SearchTokenEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(entities: List<SearchTokenEntity>)

    // === Import Upsert ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(entity: SearchTokenEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(entities: List<SearchTokenEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM entry_search_tokens WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_search_tokens WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM entry_search_tokens WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: LookupField)

    @Query("DELETE FROM entry_search_tokens")
    suspend fun clear(): Int
}
