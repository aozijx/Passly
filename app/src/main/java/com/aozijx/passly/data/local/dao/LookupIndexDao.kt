package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.LookupIndexEntity

@Dao
interface LookupIndexDao {

    // ---- search ----

    @Query("SELECT DISTINCT entryId FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE keywordHash = :keywordHash ORDER BY weight DESC")
    suspend fun searchByHash(keywordHash: ByteArray): List<String>

    @Query("SELECT DISTINCT entryId FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE keywordHash IN (:keywordHashes) ORDER BY weight DESC")
    suspend fun searchByHashes(keywordHashes: List<ByteArray>): List<String>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<LookupIndexEntity>

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LookupIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LookupIndexEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX}")
    suspend fun clear()

    // ---- maintenance ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_LOOKUP_INDEX}")
    suspend fun count(): Int
}
