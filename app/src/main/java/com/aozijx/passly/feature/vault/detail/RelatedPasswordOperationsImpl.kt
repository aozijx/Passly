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
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.RelatedPasswordOperations
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

internal class RelatedPasswordOperationsImpl
    @Inject
    constructor(
        private val entryListQueryRepository: EntryListQueryRepository,
        private val entryLinkRepository: EntryLinkRepository,
        private val logger: Logger,
    ) : RelatedPasswordOperations {
        override suspend fun candidates(sourceEntryId: EntryId): List<EntryListItem> =
            withLoggedFailure {
                val activeLogins = activeLogins()
                if (activeLogins.none { it.id == sourceEntryId }) return@withLoggedFailure emptyList()
                val activeIds = activeLogins.mapTo(hashSetOf()) { it.id }

                val relatedIds =
                    entryLinkRepository
                        .getLinks(sourceEntryId)
                        .asSequence()
                        .map { link ->
                            if (link.firstEntryId == sourceEntryId) link.secondEntryId else link.firstEntryId
                        }.filter(activeIds::contains).toSet()

                activeLogins.filter { it.id != sourceEntryId && it.id !in relatedIds }
            }

        override suspend fun bind(
            sourceEntryId: EntryId,
            targetEntryId: EntryId,
        ): AppResult<Unit> =
            withLoggedFailure {
                if (sourceEntryId == targetEntryId) return@withLoggedFailure AppResult.Failure(ValidationError())

                val activeIds = activeLogins().mapTo(hashSetOf()) { it.id }
                if (sourceEntryId !in activeIds || targetEntryId !in activeIds) {
                    return@withLoggedFailure AppResult.Failure(ValidationError())
                }

                val alreadyRelated =
                    entryLinkRepository
                        .getLinks(sourceEntryId)
                        .any { link ->
                            link.otherEndpoint(sourceEntryId) == targetEntryId
                        }
                if (alreadyRelated) return@withLoggedFailure AppResult.Success(Unit)

                val now = System.currentTimeMillis()
                val result =
                    entryLinkRepository.upsert(
                        EntryLink.create(
                            id = EntryLinkId(UUID.randomUUID().toString()),
                            firstEntryId = sourceEntryId,
                            secondEntryId = targetEntryId,
                            createdAt = now,
                        ),
                    )
                if (result is AppResult.Failure) logFailure(result.error)
                result
            }

        override suspend fun unlink(
            sourceEntryId: EntryId,
            targetEntryId: EntryId,
            linkId: EntryLinkId,
        ): AppResult<Unit> =
            withLoggedFailure {
                if (sourceEntryId == targetEntryId) return@withLoggedFailure AppResult.Failure(ValidationError())

                val activeIds = activeLogins().mapTo(hashSetOf()) { it.id }
                if (sourceEntryId !in activeIds || targetEntryId !in activeIds) {
                    return@withLoggedFailure AppResult.Failure(ValidationError())
                }

                val sourceLinks = entryLinkRepository.getLinks(sourceEntryId)
                val selectedLink = sourceLinks.firstOrNull { it.id == linkId }
                if (selectedLink == null) {
                    val samePasswordRelationExists =
                        sourceLinks.any { link ->
                            link.otherEndpoint(sourceEntryId) == targetEntryId
                        }
                    return@withLoggedFailure if (samePasswordRelationExists) {
                        AppResult.Failure(ValidationError())
                    } else {
                        AppResult.Success(Unit)
                    }
                }

                if (selectedLink.otherEndpoint(sourceEntryId) != targetEntryId) {
                    return@withLoggedFailure AppResult.Failure(ValidationError())
                }

                val result = entryLinkRepository.delete(linkId)
                if (result is AppResult.Failure) logFailure(result.error)
                result
            }

        private suspend fun activeLogins(): List<EntryListItem> = entryListQueryRepository.getActiveByType(EntryType.LOGIN)

        private suspend fun <T> withLoggedFailure(block: suspend () -> T): T =
            try {
                logger.inSpan(Operations.VaultEntryUpdate) { block() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logFailure(failure)
                throw failure
            }

        private fun logFailure(failure: Throwable) {
            logger.error(
                Events.Vault.Error,
                ErrorCodes.Security.Vault.VaultOperationFailed,
                Reasons.Application.Fallback.UnknownFailure,
                failure,
            )
        }
    }
