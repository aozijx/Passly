package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import javax.inject.Inject

internal class CreateEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val summaryCodec: EntryProfileCodec,
    private val secretFieldStore: SecretFieldStore,
    private val searchIndexWriter: EntrySearchIndexWriter,
    private val revisionWriter: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {
    suspend fun execute(entry: Entry): AppResult<EntryId> {
        val result = databaseTransactions.write("entry_create") {
            val now = clock.now()
            val entryId = entry.id.value
            val entity = EntryEntity(
                entryId = entryId,
                entryType = entry.type,
                capabilityFlags = EntryCapabilities.from(entry.secret).toDatabaseFlags(),
                otpType = entry.secret.otp?.config?.type?.name,
                summaryBlob = summaryCodec.encrypt(entry.profile, entryId),
                createdAt = now,
                updatedAt = now,
            )
            entryCommandDao().insertStrict(entity)
            secretFieldStore.replaceAll(this, entryId, entry.secret)
            searchIndexWriter.rebuildForEntry(this, entryId, entry.toLookupFields())
            revisionWriter.snapshotChanges(
                db = this,
                entryId = entryId,
                entryVersion = 1,
                summary = entry.profile,
                secret = entry.secret,
                now = now,
            )
            activityWriter.recordActivity(this, entryId, ActivityType.CREATE, now)
            if (entry.secret.toSensitiveFieldValues().isNotEmpty()) {
                activityWriter.recordActivity(this, entryId, ActivityType.SENSITIVE_CHANGE, now)
            }
            EntryId(entryId)
        }
        result.onSuccessSuspend { attachmentGarbageCollector.drain() }
        return result
    }
}
