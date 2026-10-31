package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import javax.inject.Inject

internal class CreateEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val secretFieldStore: SecretFieldStore,
    private val revisionWriter: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val postCommitCleanup: PostCommitCleanup,
) {
    suspend fun execute(entry: Entry): AppResult<EntryId> {
        return execute(entry, databaseTransactions::write)
    }

    suspend fun executeForAutofill(entry: Entry): AppResult<EntryId> {
        return execute(entry, databaseTransactions::writeForAutofill)
    }

    suspend fun executeForInbox(entry: Entry): AppResult<EntryId> {
        return execute(entry, databaseTransactions::writeForAutofill, idempotentByEntryId = true)
    }

    private suspend fun execute(
        entry: Entry,
        write: suspend (
            String,
            suspend AppDatabase.() -> EntryId,
        ) -> AppResult<EntryId>,
        idempotentByEntryId: Boolean = false,
    ): AppResult<EntryId> {
        val result = write("entry_create") {
            val entryId = entry.id.value
            if (idempotentByEntryId && entryQueryDao().getById(entryId) != null) {
                return@write EntryId(entryId)
            }
            val now = clock.now()
            val entity = EntryEntity(
                entryId = entryId,
                entryType = entry.type,
                capabilityFlags = EntryCapabilities.from(entry.secret).toDatabaseFlags(),
                otpType = entry.secret.otp?.config?.type,
                title = entry.profile.title,
                username = entry.profile.username,
                primaryUrl = entry.profile.associations.primaryUrl,
                domains = entry.profile.associations.domains,
                applicationIds = entry.profile.associations.applicationIds,
                iconName = entry.profile.icon.name,
                iconCustomReference = entry.profile.icon.customReference,
                favorite = entry.profile.favorite,
                tags = entry.profile.tags,
                iconColor = entry.profile.icon.color,
                expiresAt = entry.profile.expiresAtMs,
                createdAt = now,
                updatedAt = now,
            )
            entryCommandDao().insertStrict(entity)
            secretFieldStore.replaceAll(this, entryId, entry.secret, entry.type)
            revisionWriter.snapshotChanges(
                db = this,
                entryId = entryId,
                entryVersion = 1,
                summary = entry.profile,
                secret = entry.secret,
                entryType = entry.type,
                now = now,
            )
            attachmentGarbageCollector.scheduleInTransaction(this)
            activityWriter.recordActivity(this, entryId, ActivityType.CREATE, now,
                EntryActivityContext.ContentRevision(EntryVersion.INITIAL))
            EntryId(entryId)
        }
        return postCommitCleanup.afterCommit(result) {
            attachmentGarbageCollector.drain()
        }
    }
}
