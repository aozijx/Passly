package com.aozijx.passly.data.local.dao.entry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntrySecretEntity

@Dao
interface EntrySecretCommandDao {

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(secret: EntrySecretEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(secrets: List<EntrySecretEntity>)

    // === Update ===

    @Query("UPDATE entry_secrets SET secretBlob = :secretBlob, highSensitivityBlob = :highSensitivityBlob WHERE entryId = :entryId")
    suspend fun updateBlob(
        entryId: String,
        secretBlob: ByteArray,
        highSensitivityBlob: ByteArray?
    ): Int

    // === Maintenance API ===

    @Query("DELETE FROM entry_secrets WHERE entryId = :entryId")
    suspend fun delete(entryId: String)
}
