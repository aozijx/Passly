package com.aozijx.passly.data.local.database.dao.secret

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity

@Dao
interface SecretFieldQueryDao {

    @Query(
        """
        SELECT * FROM entry_secret_fields
        WHERE entryId = :entryId AND fieldKey = :fieldKey
        LIMIT 1
        """
    )
    suspend fun getField(
        entryId: String,
        fieldKey: String
    ): EntrySecretFieldEntity?

    @Query(
        """
        SELECT * FROM entry_secret_fields
        WHERE entryId = :entryId
        ORDER BY fieldKey ASC
        """
    )
    suspend fun getAll(entryId: String): List<EntrySecretFieldEntity>

    @Query(
        """
        SELECT fieldKey FROM entry_secret_fields
        WHERE entryId = :entryId
        ORDER BY fieldKey ASC
        """
    )
    suspend fun getKeys(entryId: String): List<String>

    @Query(
        """
        SELECT * FROM entry_secret_fields
        WHERE entryId IN (:entryIds)
        ORDER BY entryId ASC, fieldKey ASC
        """
    )
    suspend fun getAllForEntries(entryIds: List<String>): List<EntrySecretFieldEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM entry_secret_fields WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean
}
