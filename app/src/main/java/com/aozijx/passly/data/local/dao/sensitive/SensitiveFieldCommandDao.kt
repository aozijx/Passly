package com.aozijx.passly.data.local.dao.sensitive

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity

@Dao
interface SensitiveFieldCommandDao {
    @Upsert
    suspend fun upsert(field: EntrySensitiveFieldEntity)

    @Query(
        """
        DELETE FROM entry_sensitive_fields
        WHERE entryId = :entryId AND fieldKey = :fieldKey
        """
    )
    suspend fun delete(
        entryId: String,
        fieldKey: String
    ): Int
}
