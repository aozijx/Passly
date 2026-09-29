package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.datastore.RevisionHistoryMaintenanceStore
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Runs the destructive revision-format reset exactly once for this installation. */
@Singleton
internal class InitializeEntryRevisionHistory @Inject constructor(
    private val maintenanceStore: RevisionHistoryMaintenanceStore,
    private val databaseTransactions: DatabaseTransactionRunner,
    private val rebuilder: EntryRevisionHistoryRebuilder,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {
    private val mutex = Mutex()

    suspend fun execute(): AppResult<Unit> = mutex.withLock {
        val completedGeneration = AppResult.runSuspendCatching {
            maintenanceStore.completedGeneration()
        }
        if (completedGeneration is AppResult.Failure) return@withLock completedGeneration
        if ((completedGeneration as AppResult.Success).data >= CURRENT_GENERATION) {
            return@withLock AppResult.success(Unit)
        }
        val rebuild = databaseTransactions.write("entry_revision_history_initialize") {
            rebuilder.rebuildInTransaction(this, clock.now())
            attachmentGarbageCollector.scheduleInTransaction(this)
        }
        when (rebuild) {
            is AppResult.Failure -> rebuild
            is AppResult.Success -> AppResult.runSuspendCatching {
                maintenanceStore.markCompleted(CURRENT_GENERATION)
                attachmentGarbageCollector.drain()
            }
        }
    }

    private companion object {
        const val CURRENT_GENERATION = 1
    }
}
