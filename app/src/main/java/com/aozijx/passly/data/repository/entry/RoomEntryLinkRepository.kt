package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.model.entity.EntryLinkEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.link.EntryLink
import com.aozijx.passly.domain.entry.model.link.EntryLinkId
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import com.aozijx.passly.domain.entry.repository.EntryLinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryLinkRepository @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState
) : EntryLinkRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) {
                flowOf(emptyList())
            } else {
                sessionManager.observeFlow {
                    entryLinkQueryDao().observeByEntryId(entryId.value)
                        .map { links -> links.map { it.toDomain() } }
                }
            }
        }

    override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
        if (!sessionState.hasFullSecureSessionAccess()) {
            emptyList()
        } else {
            sessionManager.query {
                entryLinkQueryDao().getByEntryId(entryId.value).map { it.toDomain() }
            }
        }

    override suspend fun upsert(link: EntryLink): AppResult<Unit> =
        transactionRunner.write("entry-link.upsert") {
            entryLinkCommandDao().upsert(link.toEntity())
        }

    override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> =
        transactionRunner.write("entry-link.delete") {
            entryLinkCommandDao().deleteById(linkId.value)
            Unit
        }

    private fun EntryLinkEntity.toDomain(): EntryLink = EntryLink.create(
        id = EntryLinkId(linkId),
        sourceEntryId = EntryId(sourceEntryId),
        targetEntryId = EntryId(targetEntryId),
        relationType = EntryRelationType.valueOf(relationType),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun EntryLink.toEntity(): EntryLinkEntity = EntryLinkEntity(
        linkId = id.value,
        sourceEntryId = sourceEntryId.value,
        targetEntryId = targetEntryId.value,
        relationType = relationType.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
