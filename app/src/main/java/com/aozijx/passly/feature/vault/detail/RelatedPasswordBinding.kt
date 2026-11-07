package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

internal class RelatedPasswordBinding
    @Inject
    constructor(
        private val entryListQueryRepository: EntryListQueryRepository,
        private val entryLinkRepository: EntryLinkRepository,
        private val logger: Logger,
    ) {
        suspend fun candidates(sourceEntryId: EntryId): List<EntryListItem> =
            withLoggedFailure {
                val activePasswords = activePasswords()
                if (activePasswords.none { it.id == sourceEntryId }) return@withLoggedFailure emptyList()

                val relatedIds =
                    entryLinkRepository
                        .getLinks(sourceEntryId)
                        .asSequence()
                        .filter { it.relationType == EntryRelationType.RELATED_TO }
                        .map { link ->
                            if (link.sourceEntryId == sourceEntryId) link.targetEntryId else link.sourceEntryId
                        }.toSet()

                activePasswords.filter { it.id != sourceEntryId && it.id !in relatedIds }
            }

        suspend fun bind(
            sourceEntryId: EntryId,
            targetEntryId: EntryId,
        ): AppResult<Unit> =
            withLoggedFailure {
                if (sourceEntryId == targetEntryId) return@withLoggedFailure AppResult.Failure(ValidationError())

                val activeIds = activePasswords().mapTo(hashSetOf()) { it.id }
                if (sourceEntryId !in activeIds || targetEntryId !in activeIds) {
                    return@withLoggedFailure AppResult.Failure(ValidationError())
                }

                val alreadyRelated =
                    entryLinkRepository
                        .getLinks(sourceEntryId)
                        .any { link ->
                            link.relationType == EntryRelationType.RELATED_TO &&
                                (link.sourceEntryId == targetEntryId || link.targetEntryId == targetEntryId)
                        }
                if (alreadyRelated) return@withLoggedFailure AppResult.Success(Unit)

                val now = System.currentTimeMillis()
                entryLinkRepository.upsert(
                    EntryLink.create(
                        id = EntryLinkId(UUID.randomUUID().toString()),
                        sourceEntryId = sourceEntryId,
                        targetEntryId = targetEntryId,
                        relationType = EntryRelationType.RELATED_TO,
                        createdAt = now,
                    ),
                )
            }

        private suspend fun activePasswords(): List<EntryListItem> =
            entryListQueryRepository
                .getActiveByType(EntryType.LOGIN)
                .filter { it.entryType == EntryType.LOGIN && it.deletedAt == null }

        private suspend fun <T> withLoggedFailure(block: suspend () -> T): T =
            try {
                logger.inSpan(Operations.VaultEntryUpdate) { block() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logger.error(
                    Events.Vault.Error,
                    ErrorCodes.Security.VaultOperationFailed,
                    Reasons.UnknownFailure,
                    failure,
                )
                throw failure
            }
    }
