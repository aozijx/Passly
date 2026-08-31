package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.mapper.entry.hasEntryCapability
import com.aozijx.passly.data.mapper.entry.mergePreservedFields
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import javax.inject.Inject

internal class UpdateEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val secretFieldStore: SecretFieldStore,
    private val searchIndexWriter: EntrySearchIndexWriter,
    private val revisionWriter: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val entryResourceCleaner: EntryResourceCleaner,
) {
    suspend fun execute(id: String, expectedVersion: Int, changes: EntryUpdate): AppResult<Unit> {
        var replacedIconPath: String? = null
        var currentIconPath: String? = null
        val result = databaseTransactions.write("entry_update") {
            val entity = entryQueryDao().getById(id) ?: throw NotFound()
            val oldProfile = EntryProfileMapper.fromEntity(entity)
            val oldSecret = secretFieldStore.readAll(this, id)
            val newProfile = changes.profile ?: oldProfile
            replacedIconPath = oldProfile.icon.customReference
            currentIconPath = newProfile.icon.customReference
            val newSecret = changes.secret?.mergePreservedFields(oldSecret) ?: oldSecret
            require(newSecret.credential.kind == entity.entryType.credentialKind) {
                "${entity.entryType} cannot contain ${newSecret.credential.kind} credentials"
            }

            val now = clock.now()
            val hasAttachments = entity.capabilityFlags.hasEntryCapability(EntryCapability.ATTACHMENTS)
            val flags = EntryCapabilities.from(newSecret, hasAttachments).toDatabaseFlags()
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
                newSecret.otp?.config?.type?.name,
                now,
            )
            databaseTransactions.checkAffectedRows(affected)

            if (changes.secret != null) {
                secretFieldStore.replaceAll(this, id, newSecret)
            }

            if (changes.profile != null || changes.secret != null) {
                val updated = EntryAssembler.assembleFromDatabase(
                    entity.copy(version = expectedVersion + 1, updatedAt = now),
                    newProfile,
                    newSecret,
                )
                searchIndexWriter.rebuildForEntry(this, id, updated.toLookupFields())
            }

            revisionWriter.snapshotChanges(
                db = this,
                entryId = id,
                entryVersion = expectedVersion + 1,
                summary = newProfile,
                secret = newSecret,
                now = now,
            )
            activityWriter.recordActivity(this, id, ActivityType.UPDATE, now)
            if (changes.secret != null) {
                activityWriter.recordActivity(this, id, ActivityType.SENSITIVE_CHANGE, now)
            }
        }
        result.onSuccessSuspend {
            attachmentGarbageCollector.drain()
            entryResourceCleaner.cleanReplacedIcon(replacedIconPath, currentIconPath)
        }
        return result
    }
}
