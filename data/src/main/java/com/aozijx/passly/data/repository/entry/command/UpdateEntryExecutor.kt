package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.domain.entry.model.EntryUpdate
import javax.inject.Inject

internal class UpdateEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val transaction: EntryUpdateTransaction,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val entryResourceCleaner: EntryResourceCleaner,
    private val postCommitCleanup: PostCommitCleanup,
) {
    suspend fun execute(id: String, expectedVersion: Int, changes: EntryUpdate): AppResult<Unit> {
        val result = databaseTransactions.write("entry_update") {
            transaction.update(this, id, expectedVersion, changes, clock.now()).also {
                if (it is EntryContentUpdate.Changed) attachmentGarbageCollector.scheduleInTransaction(this)
            }
        }
        val cleaned = postCommitCleanup.afterCommit(result) { update ->
            if (update is EntryContentUpdate.Changed) {
                postCommitCleanup.run { attachmentGarbageCollector.drain() }
                postCommitCleanup.run {
                    entryResourceCleaner.cleanReplacedIcon(update.previousIcon, update.currentIcon)
                }
            }
        }
        return cleaned.map { Unit }
    }
}
