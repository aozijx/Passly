package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.KeyEnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyEnvelopeDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KeyEnvelopeEntity>>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} WHERE envelopeId = :envelopeId LIMIT 1")
    suspend fun getById(envelopeId: String): KeyEnvelopeEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} WHERE type = :type")
    suspend fun getByType(type: Int): List<KeyEnvelopeEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} WHERE envelopeId = :envelopeId)")
    suspend fun exists(envelopeId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE}")
    suspend fun count(): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(envelope: KeyEnvelopeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(envelopes: List<KeyEnvelopeEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} WHERE envelopeId = :envelopeId")
    suspend fun deleteById(envelopeId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE} WHERE type = :type")
    suspend fun deleteByType(type: Int)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_KEY_ENVELOPE}")
    suspend fun clear(): Int
}