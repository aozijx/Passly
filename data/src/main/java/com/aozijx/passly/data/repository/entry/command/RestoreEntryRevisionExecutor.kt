package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.EntryRevisionRestoreTransaction
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import javax.inject.Inject

internal class RestoreEntryRevisionExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val transaction: EntryRevisionRestoreTransaction,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val entryResourceCleaner: EntryResourceCleaner,
) {
    suspend fun execute(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        expectedVersion: EntryVersion,
        permit: AuthorizationPermit?,
    ): AppResult<EntryVersion> {
        val result = databaseTransactions.write("entry_revision_restore") {
            transaction.restoreWithResources(
                db = this,
                entryId = entryId,
                revisionId = revisionId,
                expectedVersion = expectedVersion,
                permit = permit,
                now = clock.now(),
            ).also {
                attachmentGarbageCollector.scheduleInTransaction(this)
            }
        }
        result.onSuccessSuspend { restored ->
            attachmentGarbageCollector.drain()
            entryResourceCleaner.cleanReplacedIcon(
                oldPath = restored.previousIconPath,
                newPath = restored.restoredIconPath,
            )
        }
        return result.map { it.version }
    }
}
