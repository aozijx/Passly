package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.toHighSensitivitySecret
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject

class SensitiveFieldPersistence @Inject constructor(
    private val codec: SensitiveFieldCodec,
    private val clock: Clock
) {
    suspend fun readAllForMutation(
        db: AppDatabase,
        entryId: String
    ): EntryHighSensitivitySecret {
        return db.sensitiveFieldQueryDao().getFields(entryId).associate { entity ->
            val key = SensitiveFieldKey.valueOf(entity.fieldKey)
            key to codec.decryptProvisioned(entryId, key, entity.valueCipher)
        }.toHighSensitivitySecret()
    }

    suspend fun readAllUnlocked(
        db: AppDatabase,
        entryId: String
    ): EntryHighSensitivitySecret {
        return db.sensitiveFieldQueryDao().getFields(entryId).associate { entity ->
            val key = SensitiveFieldKey.valueOf(entity.fieldKey)
            key to codec.decrypt(entryId, key, entity.valueCipher)
        }.toHighSensitivitySecret()
    }

    suspend fun replaceAll(
        db: AppDatabase,
        entryId: String,
        secret: EntryHighSensitivitySecret
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
