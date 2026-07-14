package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.domain.model.lookup.LookupField

@Dao
interface LookupIndexDao {

    // ---- search ----

    @Query("SELECT entryId FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE keywordHash = :hash AND gramLength = :length AND field IN (:fields) ORDER BY weight DESC")
    suspend fun searchByHash(hash: ByteArray, length: Int, fields: List<LookupField>): List<String>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<LookupIndexEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun count(): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LookupIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LookupIndexEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: LookupField)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun clear()
}