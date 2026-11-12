package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.data.repository.entry.command.PostCommitCleanup
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomEntryLinkRepository
    @Inject
    constructor(
        private val databaseTransactions: DatabaseTransactionRunner,
        private val databaseSession: AppDatabaseSession,
        private val revisionWriter: EntryRevisionWriter,
        private val secretFieldStore: SecretFieldStore,
        private val clock: DatabaseClock,
        private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
        private val postCommitCleanup: PostCommitCleanup,
    ) : EntryLinkRepository {
        override fun observeAll(): Flow<List<EntryLink>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { emptyList() }) {
                entryLinkQueryDao()
                    .observeAll()
                    .map { links -> links.map { it.toDomain() } }
            }

        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { emptyList() }) {
                entryLinkQueryDao()
                    .observeByEntryId(entryId.value)
                    .map { links -> links.map { it.toDomain() } }
            }

        override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
            databaseSession.queryFullVault(
                onDenied = { emptyList() },
            ) {
                entryLinkQueryDao().getByEntryId(entryId.value).map { it.toDomain() }
            }

        override suspend fun getAll(): List<EntryLink> =
            databaseSession.queryFullVault(
                onDenied = { emptyList() },
            ) {
                entryLinkQueryDao().getAll().map { it.toDomain() }
            }

        override suspend fun upsert(link: EntryLink): AppResult<Unit> {
            val result =
                databaseTransactions.write("entry_link_upsert") {
                    val endpoints =
                        entryQueryDao()
                            .getByIds(
                                listOf(link.firstEntryId.value, link.secondEntryId.value),
                            ).associateBy { it.entryId }
                    val first = endpoints[link.firstEntryId.value]
                    val second = endpoints[link.secondEntryId.value]
                    if (
                        first == null || second == null ||
                        first.deletedAt != null || second.deletedAt != null
                    ) {
                        throw ValidationError()
                    }
                    val previous = entryLinkQueryDao().getById(link.id.value)
                    if (previous != null && previous.firstEntryId == link.firstEntryId.value &&
                        previous.secondEntryId == link.secondEntryId.value
                    ) {
                        return@write Unit
                    }
                    entryLinkCommandDao().upsert(link.toEntity())
                    val affectedEntryIds =
                        buildSet {
                            add(link.firstEntryId.value)
                            add(link.secondEntryId.value)
                            previous?.let {
                                add(it.firstEntryId)
                                add(it.secondEntryId)
                            }
                        }
                    val now = clock.now()
                    affectedEntryIds.forEach {
                        revisionWriter.snapshotCurrent(this, it, now, secretFieldStore)
                    }
                    attachmentGarbageCollector.scheduleInTransaction(this)
                }
            return postCommitCleanup.afterCommit(result) {
                attachmentGarbageCollector.drain()
            }
        }

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> {
            val result =
                databaseTransactions.write("entry_link_delete") {
                    val link = entryLinkQueryDao().getById(linkId.value)
                    entryLinkCommandDao().deleteById(linkId.value)
                    if (link != null) {
                        val now = clock.now()
                        setOf(link.firstEntryId, link.secondEntryId).forEach {
                            revisionWriter.snapshotCurrent(this, it, now, secretFieldStore)
                        }
                        attachmentGarbageCollector.scheduleInTransaction(this)
                    }
                    Unit
                }
            return postCommitCleanup.afterCommit(result) {
                attachmentGarbageCollector.drain()
            }
        }

        private fun EntryLinkEntity.toDomain(): EntryLink =
            EntryLink.create(
                id = EntryLinkId(linkId),
                firstEntryId = EntryId(firstEntryId),
                secondEntryId = EntryId(secondEntryId),
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun EntryLink.toEntity(): EntryLinkEntity =
            EntryLinkEntity(
                linkId = id.value,
                firstEntryId = firstEntryId.value,
                secondEntryId = secondEntryId.value,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
