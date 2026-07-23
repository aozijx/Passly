package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.domain.model.lookup.LookupField

@Dao
interface LookupIndexDao {

    // ---- search (single token) ----

    @Query("SELECT entryId FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE keywordHash = :hash AND gramLength = :length AND field IN (:fields) ORDER BY weight DESC")
    suspend fun searchByHash(hash: ByteArray, length: Int, fields: List<LookupField>): List<String>

    @RawQuery(observedEntities = [LookupIndexEntity::class])
    suspend fun searchByTokenIntersection(query: SupportSQLiteQuery): List<String>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<LookupIndexEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count / status ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT entryId) FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun countDistinctEntryIds(): Int

    // === Strict Insert ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entity: LookupIndexEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(entities: List<LookupIndexEntity>)

    // === Import Upsert ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(entity: LookupIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(entities: List<LookupIndexEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: LookupField)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun clear(): Int
}
