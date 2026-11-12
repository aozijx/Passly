package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.datastore.RevisionHistoryMaintenanceStore
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import javax.inject.Inject
import javax.inject.Singleton

/** Runs the destructive revision-format reset exactly once for this installation. */
@Singleton
internal class InitializeEntryRevisionHistory
    @Inject
    constructor(
        private val maintenanceStore: RevisionHistoryMaintenanceStore,
        private val databaseTransactions: DatabaseTransactionRunner,
        private val rebuilder: EntryRevisionHistoryRebuilder,
        private val clock: DatabaseClock,
        private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
        private val entryResourceCleaner: EntryResourceCleaner,
        private val postCommitCleanup: PostCommitCleanup,
    ) {
        private val initializationFlow =
            EntryRevisionHistoryInitializationFlow(
                currentGeneration = CURRENT_GENERATION,
                completedGeneration = {
                    AppResult.runSuspendCatching { maintenanceStore.completedGeneration() }
                },
                rebuild = {
                    databaseTransactions.write("entry_revision_history_initialize") {
                        val oldIcons = entryRevisionQueryDao().getAllIconReferences()
                        rebuilder.rebuildInTransaction(this, clock.now())
                        oldIcons.forEach(entryResourceCleaner::discardUnreferencedIcon)
                        attachmentGarbageCollector.scheduleInTransaction(this)
                    }
                },
                markCompleted = { generation ->
                    AppResult.runSuspendCatching { maintenanceStore.markCompleted(generation) }
                },
                afterCommitCleanup = {
                    postCommitCleanup.run { attachmentGarbageCollector.drain() }
                    postCommitCleanup.run { entryResourceCleaner.drainPendingIcons() }
                },
            )

        suspend fun execute(): AppResult<Unit> = initializationFlow.execute()

        private companion object {
            const val CURRENT_GENERATION = 1
        }
    }
