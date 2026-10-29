package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.mapper.entry.mergeSensitiveFields
import com.aozijx.passly.data.mapper.entry.SensitiveFieldUpdate
import com.aozijx.passly.data.mapper.entry.toBundleSecret
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldUpdates
import com.aozijx.passly.data.codec.sensitiveFieldKeyFromStorageKey
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
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
    suspend fun replaceAll(db: AppDatabase, entryId: String, secret: EntrySecret, entryType: EntryType) {
        db.secretFieldCommandDao().deleteAll(entryId)
        val now = clock.now()
        val rows = mutableListOf<EntrySecretFieldEntity>()
        secret.toSensitiveFieldValues().forEach { (key, value) ->
            rows += EntrySecretFieldEntity(
                entryId = entryId,
                fieldKey = key.toStorageKey(),
                valueCipher = fieldCodec.encrypt(entryId, key, value),
                keyVersion = 1,
                updatedAt = now,
            )
        }
        rows += EntrySecretFieldEntity(
            entryId = entryId,
            fieldKey = SecretBundleCodec.FIELD_KEY,
            valueCipher = bundleCodec.encrypt(secret.toBundleSecret(), entryId, entryType),
            keyVersion = 1,
            updatedAt = now,
        )
        rows.forEach { db.secretFieldCommandDao().upsert(it) }
    }

    /**
     * Updates an existing entry without reading or rewriting untouched high-sensitivity rows.
     */
    suspend fun updateExisting(db: AppDatabase, entryId: String, secret: EntrySecret, entryType: EntryType) {
        val now = clock.now()
        secret.toSensitiveFieldUpdates().forEach { (key, update) ->
            when (update) {
                SensitiveFieldUpdate.Preserve -> Unit
                SensitiveFieldUpdate.Clear -> db.secretFieldCommandDao().delete(
                    entryId,
                    key.toStorageKey(),
                )
                is SensitiveFieldUpdate.Replace -> db.secretFieldCommandDao().upsert(
                    EntrySecretFieldEntity(
                        entryId = entryId,
                        fieldKey = key.toStorageKey(),
                        valueCipher = fieldCodec.encrypt(entryId, key, update.value),
                        keyVersion = 1,
                        updatedAt = now,
                    ),
                )
            }
        }
        db.secretFieldCommandDao().upsert(
            EntrySecretFieldEntity(
                entryId = entryId,
                fieldKey = SecretBundleCodec.FIELD_KEY,
                valueCipher = bundleCodec.encrypt(secret.toBundleSecret(), entryId, entryType),
                keyVersion = 1,
                updatedAt = now,
            ),
        )
    }

    /** Low-sensitivity aggregate; every field-level value is `null`. */
    suspend fun readBundle(db: AppDatabase, entryId: String, entryType: EntryType): EntrySecret {
        val bundle = db.secretFieldQueryDao().getField(entryId, SecretBundleCodec.FIELD_KEY)
            ?: return emptyBundleFor(entryType)
        return bundleCodec.decrypt(bundle.valueCipher, entryId, entryType)
    }

    /** Complete secret assembled from the bundle plus every field-level ciphertext row. */
    suspend fun readAll(db: AppDatabase, entryId: String, entryType: EntryType): EntrySecret {
        val rows = db.secretFieldQueryDao().getAll(entryId)
        val bundle = rows.firstOrNull { it.fieldKey == SecretBundleCodec.FIELD_KEY }
            ?.let { bundleCodec.decrypt(it.valueCipher, entryId, entryType) }
            ?: emptyBundleFor(entryType)
        val fields = rows.mapNotNull { row ->
            if (row.fieldKey == SecretBundleCodec.FIELD_KEY) return@mapNotNull null
            val key = sensitiveFieldKeyFromStorageKey(row.fieldKey)
            key to fieldCodec.decrypt(entryId, key, row.valueCipher)
        }.toMap()
        return bundle.mergeSensitiveFields(fields)
    }

    /** Decrypts a single field-level value without materializing the others. */
    suspend fun readField(db: AppDatabase, entryId: String, key: SensitiveFieldKey): String? {
        val row = db.secretFieldQueryDao().getField(entryId, key.toStorageKey()) ?: return null
        return fieldCodec.decrypt(entryId, key, row.valueCipher)
    }

    /** Reads only the OTP structure and its secret, leaving every unrelated secret encrypted. */
    suspend fun readOtpConfig(db: AppDatabase, entryId: String): OtpConfig? {
        val entry = db.entryQueryDao().getById(entryId) ?: return null
        if (entry.deletedAt != null || entry.entryType.credentialKind != EntryCredentialKind.OTP) return null
        val bundle = readBundle(db, entryId, entry.entryType)
        val config = bundle.otp?.config ?: return null
        val secret = readField(db, entryId, SensitiveFieldKey.OTP_SECRET)
        return config.copy(secret = secret)
    }

    private fun emptyBundleFor(entryType: EntryType): EntrySecret {
        require(entryType.credentialKind == EntryCredentialKind.NONE) {
            "Secret bundle is missing for entry type $entryType"
        }
        return EntrySecret()
    }
}
