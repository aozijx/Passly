package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomEntryLinkRepository @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val revisionHelper: EntryRevisionWriter,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) : EntryLinkRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<EntryLink>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) {
                flowOf(emptyList())
            } else {
                databaseSession.observeFlow {
                    entryLinkQueryDao().observeAll()
                        .map { links -> links.map { it.toDomain() } }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) {
                flowOf(emptyList())
            } else {
                databaseSession.observeFlow {
                    entryLinkQueryDao().observeByEntryId(entryId.value)
                        .map { links -> links.map { it.toDomain() } }
                }
            }
        }

    override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
        if (!sessionState.hasFullSecureSessionAccess()) {
            emptyList()
        } else {
            databaseSession.query {
                entryLinkQueryDao().getByEntryId(entryId.value).map { it.toDomain() }
            }
        }

    override suspend fun getAll(): List<EntryLink> =
        if (!sessionState.hasFullSecureSessionAccess()) {
            emptyList()
        } else {
            databaseSession.query {
                entryLinkQueryDao().getAll().map { it.toDomain() }
            }
        }

    override suspend fun upsert(link: EntryLink): AppResult<Unit> {
        val result = databaseTransactions.write("entry_link_upsert") {
            val endpoints = entryQueryDao().getByIds(
                listOf(link.sourceEntryId.value, link.targetEntryId.value)
            ).associateBy { it.entryId }
            val source = endpoints[link.sourceEntryId.value]
            val target = endpoints[link.targetEntryId.value]
            if (
                source == null || target == null ||
                source.deletedAt != null || target.deletedAt != null ||
                !EntryLinkPolicy.isAllowed(
                    relationType = link.relationType,
                    sourceType = source.entryType,
                    targetType = target.entryType,
                )
            ) {
                throw ValidationError()
            }
            val previous = entryLinkQueryDao().getById(link.id.value)
            entryLinkCommandDao().upsert(link.toEntity())
            val affectedEntryIds = buildSet {
                add(link.sourceEntryId.value)
                add(link.targetEntryId.value)
                previous?.let {
                    add(it.sourceEntryId)
                    add(it.targetEntryId)
                }
            }
            val now = clock.now()
            affectedEntryIds.forEach {
                revisionHelper.snapshotCurrent(this, it, now)
            }
        }
        result.onSuccessSuspend { attachmentGarbageCollector.drain() }
        return result
    }

    override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> {
        val result = databaseTransactions.write("entry_link_delete") {
            val link = entryLinkQueryDao().getById(linkId.value)
            entryLinkCommandDao().deleteById(linkId.value)
            if (link != null) {
                val now = clock.now()
                setOf(link.sourceEntryId, link.targetEntryId).forEach {
                    revisionHelper.snapshotCurrent(this, it, now)
                }
            }
            Unit
        }
        result.onSuccessSuspend { attachmentGarbageCollector.drain() }
        return result
    }

    private fun EntryLinkEntity.toDomain(): EntryLink = EntryLink.create(
        id = EntryLinkId(linkId),
        sourceEntryId = EntryId(sourceEntryId),
        targetEntryId = EntryId(targetEntryId),
        relationType = relationType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun EntryLink.toEntity(): EntryLinkEntity = EntryLinkEntity(
        linkId = id.value,
        sourceEntryId = sourceEntryId.value,
        targetEntryId = targetEntryId.value,
        relationType = relationType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
