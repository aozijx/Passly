package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.data.local.database.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject

class SensitiveFieldStore @Inject internal constructor(
    private val codec: SensitiveFieldCodec,
    private val clock: DatabaseClock
) {
    suspend fun replaceAll(
        db: AppDatabase,
        entryId: String,
        secret: EntrySecret
    ) {
        db.sensitiveFieldCommandDao().deleteAll(entryId)
        val now = clock.now()
        secret.toSensitiveFieldValues().forEach { (key, value) ->
            db.sensitiveFieldCommandDao().upsert(
                EntrySensitiveFieldEntity(
                    entryId = entryId,
                    fieldKey = key.name,
                    valueCipher = codec.encrypt(entryId, key, value),
                    keyVersion = 1,
                    updatedAt = now
                )
            )
        }
    }
}
