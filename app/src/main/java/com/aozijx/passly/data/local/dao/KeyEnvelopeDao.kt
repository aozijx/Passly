package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.KeyEnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyEnvelopeDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KeyEnvelopeEntity>>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} WHERE envelopeId = :envelopeId LIMIT 1")
    suspend fun getById(envelopeId: String): KeyEnvelopeEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} WHERE type = :type")
    suspend fun getByType(type: Int): List<KeyEnvelopeEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} WHERE envelopeId = :envelopeId)")
    suspend fun exists(envelopeId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES}")
    suspend fun count(): Int

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(envelope: KeyEnvelopeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(envelopes: List<KeyEnvelopeEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} WHERE envelopeId = :envelopeId")
    suspend fun deleteById(envelopeId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES} WHERE type = :type")
    suspend fun deleteByType(type: Int)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_KEY_ENVELOPES}")
    suspend fun clear()
}
