package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.sensitiveFieldKeyFromStorageKey
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.entry.hasEntryCapability
import com.aozijx.passly.data.mapper.entry.hasStoredChanges
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import javax.inject.Inject

internal sealed interface EntryContentUpdate {
    data object Unchanged : EntryContentUpdate
    data class Changed(val previousIcon: String?, val currentIcon: String?) : EntryContentUpdate
}

/** Database-only content rewrite; the caller owns the transaction and resource cleanup. */
internal class EntryUpdateTransaction @Inject constructor(
    private val secretFieldStore: SecretFieldStore,
    private val revisionWriter: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
) {
    suspend fun update(
        db: AppDatabase,
        id: String,
        expectedVersion: Int,
        changes: EntryUpdate,
        now: Long,
    ): EntryContentUpdate = with(db) {
        val entity = entryQueryDao().getById(id) ?: throw NotFound()
        if (entity.version != expectedVersion) throw Conflict()
        val oldProfile = EntryProfileMapper.fromEntity(entity)
        val newProfile = changes.profile ?: oldProfile
        val storedSecret = secretFieldStore.readBundle(this, id, entity.entryType)
        val newSecret = changes.secret ?: storedSecret
        val storedKeys = secretFieldQueryDao().getKeys(id).mapNotNullTo(linkedSetOf()) { key ->
            if (key == SecretBundleCodec.FIELD_KEY) null else sensitiveFieldKeyFromStorageKey(key)
        }
        if (oldProfile == newProfile && !newSecret.hasStoredChanges(storedSecret, storedKeys)) {
            return@with EntryContentUpdate.Unchanged
        }
        require(newSecret.credential.kind == entity.entryType.credentialKind) {
            "${entity.entryType} cannot contain ${newSecret.credential.kind} credentials"
        }

        if (changes.secret != null) {
            secretFieldStore.updateExisting(this, id, newSecret, entity.entryType)
        }
        val sensitiveKeys = secretFieldQueryDao().getKeys(id).mapNotNullTo(linkedSetOf()) { key ->
            if (key == SecretBundleCodec.FIELD_KEY) null else sensitiveFieldKeyFromStorageKey(key)
        }
        val hasAttachments = entity.capabilityFlags.hasEntryCapability(EntryCapability.ATTACHMENTS)
        val flags = EntryCapabilities.fromStoredSecret(newSecret, sensitiveKeys, hasAttachments).toDatabaseFlags()
        val affected = entryCommandDao().optimisticUpdate(
            id,
            expectedVersion,
            newProfile.title,
            newProfile.username,
            newProfile.associations.primaryUrl,
            newProfile.associations.domains,
            newProfile.associations.applicationIds,
            newProfile.icon.name,
            newProfile.icon.customReference,
            newProfile.favorite,
            newProfile.tags,
            newProfile.icon.color,
            newProfile.expiresAtMs,
            flags,
            newSecret.otp?.config?.type,
            now,
        )
        if (affected != 1) throw Conflict()

        revisionWriter.snapshotChanges(
            db = this,
            entryId = id,
            entryVersion = expectedVersion + 1,
            summary = newProfile,
            secret = newSecret,
            entryType = entity.entryType,
            now = now,
        )
        activityWriter.recordActivity(this, id, ActivityType.UPDATE, now,
            EntryActivityContext.ContentRevision(EntryVersion(expectedVersion + 1)))
        EntryContentUpdate.Changed(oldProfile.icon.customReference, newProfile.icon.customReference)
    }
}
