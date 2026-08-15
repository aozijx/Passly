package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.mapper.entry.mergeSensitiveFields
import com.aozijx.passly.data.mapper.entry.toBundleSecret
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject

/**
 * Writes and reads the field-keyed secret rows within a caller-provided database context
 * (query or transaction). Sensitive values live in per-key ciphertext rows; the
 * low-sensitivity structure lives in the [SecretBundleCodec.FIELD_KEY] row.
 *
 * These methods must be called inside an existing `AppDatabaseSession.query`/`transaction`
 * context (or a `DatabaseTransactionRunner` write) — never from inside another session
 * call, because the session lease is not reentrant.
 */
class SecretFieldStore @Inject internal constructor(
    private val fieldCodec: SecretFieldCodec,
    private val bundleCodec: SecretBundleCodec,
    private val clock: DatabaseClock
) {

    /** Replaces every secret row (bundle + field-level) for one entry. */
    suspend fun replaceAll(db: AppDatabase, entryId: String, secret: EntrySecret) {
        db.secretFieldCommandDao().deleteAll(entryId)
        val now = clock.now()
        val rows = mutableListOf<EntrySecretFieldEntity>()
        secret.toSensitiveFieldValues().forEach { (key, value) ->
            rows += EntrySecretFieldEntity(
                entryId = entryId,
                fieldKey = key.name,
                valueCipher = fieldCodec.encrypt(entryId, key, value),
                keyVersion = 1,
                updatedAt = now,
            )
        }
        rows += EntrySecretFieldEntity(
            entryId = entryId,
            fieldKey = SecretBundleCodec.FIELD_KEY,
            valueCipher = bundleCodec.encrypt(secret.toBundleSecret(), entryId),
            keyVersion = 1,
            updatedAt = now,
        )
        rows.forEach { db.secretFieldCommandDao().upsert(it) }
    }

    /** Low-sensitivity aggregate; every field-level value is `null`. */
    suspend fun readBundle(db: AppDatabase, entryId: String): EntrySecret {
        val bundle = db.secretFieldQueryDao().getField(entryId, SecretBundleCodec.FIELD_KEY)
            ?: return EntrySecret()
        return bundleCodec.decrypt(bundle.valueCipher, entryId)
    }

    /** Complete secret assembled from the bundle plus every field-level ciphertext row. */
    suspend fun readAll(db: AppDatabase, entryId: String): EntrySecret {
        val rows = db.secretFieldQueryDao().getAll(entryId)
        val bundle = rows.firstOrNull { it.fieldKey == SecretBundleCodec.FIELD_KEY }
            ?.let { bundleCodec.decrypt(it.valueCipher, entryId) }
            ?: EntrySecret()
        val fields = rows.mapNotNull { row ->
            if (row.fieldKey == SecretBundleCodec.FIELD_KEY) return@mapNotNull null
            val key = SensitiveFieldKey.entries.firstOrNull { it.name == row.fieldKey }
                ?: return@mapNotNull null
            key to fieldCodec.decrypt(entryId, key, row.valueCipher)
        }.toMap()
        return bundle.mergeSensitiveFields(fields)
    }

    /** Decrypts a single field-level value without materializing the others. */
    suspend fun readField(db: AppDatabase, entryId: String, key: SensitiveFieldKey): String? {
        val row = db.secretFieldQueryDao().getField(entryId, key.name) ?: return null
        return fieldCodec.decrypt(entryId, key, row.valueCipher)
    }
}
