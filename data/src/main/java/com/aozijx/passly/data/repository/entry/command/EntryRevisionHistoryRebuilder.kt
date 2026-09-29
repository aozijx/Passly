package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.entry.model.EntrySecret
import javax.inject.Inject

/** Replaces all obsolete revisions with one baseline of every current entry state. */
internal class EntryRevisionHistoryRebuilder @Inject constructor(
    private val revisionWriter: EntryRevisionWriter,
    private val bundleCodec: SecretBundleCodec,
) {
    suspend fun rebuildInTransaction(db: AppDatabase, now: Long) = with(db) {
        databaseMaintenanceDao().clearRevisions()
        entryQueryDao().getAll().forEach { entry ->
            val bundleSecret = secretFieldQueryDao()
                .getField(entry.entryId, SecretBundleCodec.FIELD_KEY)
                ?.let { bundleCodec.decrypt(it.valueCipher, entry.entryId) }
                ?: EntrySecret()
            revisionWriter.snapshotChanges(
                db = this,
                entryId = entry.entryId,
                entryVersion = entry.version,
                summary = EntryProfileMapper.fromEntity(entry),
                secret = bundleSecret,
                now = now,
            )
        }
    }
}
