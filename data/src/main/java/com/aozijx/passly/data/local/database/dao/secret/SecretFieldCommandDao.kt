package com.aozijx.passly.data.local.database.dao.secret

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity

@Dao
interface SecretFieldCommandDao {

    @Upsert
    suspend fun upsert(field: EntrySecretFieldEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(fields: List<EntrySecretFieldEntity>)

    @Query("DELETE FROM entry_secret_fields WHERE entryId = :entryId")
    suspend fun deleteAll(entryId: String): Int

    @Query(
        """
        DELETE FROM entry_secret_fields
        WHERE entryId = :entryId AND fieldKey = :fieldKey
        """
    )
    suspend fun delete(entryId: String, fieldKey: String): Int
}
