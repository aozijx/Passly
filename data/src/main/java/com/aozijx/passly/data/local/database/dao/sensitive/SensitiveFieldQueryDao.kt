package com.aozijx.passly.data.local.database.dao.sensitive

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntrySensitiveFieldEntity

@Dao
interface SensitiveFieldQueryDao {
    @Query("SELECT * FROM entry_sensitive_fields WHERE entryId = :entryId ORDER BY fieldKey ASC")
    suspend fun getFields(entryId: String): List<EntrySensitiveFieldEntity>

    @Query(
        """
        SELECT fieldKey FROM entry_sensitive_fields
        WHERE entryId = :entryId
        ORDER BY fieldKey ASC
        """
    )
    suspend fun getKeys(entryId: String): List<String>

    @Query(
        """
        SELECT * FROM entry_sensitive_fields
        WHERE entryId = :entryId AND fieldKey = :fieldKey
        LIMIT 1
        """
    )
    suspend fun getField(
        entryId: String,
        fieldKey: String
    ): EntrySensitiveFieldEntity?
}
