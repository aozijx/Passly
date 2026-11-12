package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryDetailData
import com.aozijx.passly.domain.entry.model.query.EntryDetailSnapshot
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.domain.entry.port.EntryDetailQuery
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class EntryDetailQueryImpl
    @Inject
    constructor(
        private val entryQueryRepository: EntryQueryRepository,
        private val sensitiveFieldRepository: SensitiveFieldRepository,
        private val entryLinkRepository: EntryLinkRepository,
    ) : EntryDetailQuery {
        override suspend fun open(entryId: EntryId): EntryDetailSnapshot? {
            val entry = entryQueryRepository.getById(entryId) ?: return null
            return coroutineScope {
                val detail = async { snapshot(entry) }
                val relations = async { loadRelations(entry) }
                EntryDetailSnapshot(
                    detail = detail.await(),
                    relations = relations.await(),
                )
            }
        }

        override suspend fun reloadRelations(entryId: EntryId): EntryRelationsSnapshot? {
            val entry = entryQueryRepository.getById(entryId) ?: return null
            return loadRelations(entry)
        }

        override suspend fun snapshot(entry: Entry): EntryDetailData =
            EntryDetailData(
                entry = entry,
                sensitiveFieldKeys = sensitiveFieldRepository.getPresence(entry.id).keys,
            )

        private suspend fun loadRelations(entry: Entry): EntryRelationsSnapshot {
            val links = entryLinkRepository.getAll()
            val accountGroupIds = resolveAccountGroupEntryIds(entry.id, entry.type, links)
            val directPasswordLinks = resolveDirectPasswordLinks(entry, links)
            val directRelatedIds = resolveDirectRelatedEntryIds(entry.id, links)
            val requestedIds = (accountGroupIds + directRelatedIds).distinct()
            val entriesById = entryQueryRepository.getByIds(requestedIds).associateBy(Entry::id)
            val relatedPasswords =
                directPasswordLinks.mapNotNull { link ->
                    val targetEntryId = link.otherEndpoint(entry.id) ?: return@mapNotNull null
                    val target = entriesById[targetEntryId] ?: return@mapNotNull null
                    if (target.type != EntryType.LOGIN || target.deletedAt != null) return@mapNotNull null
                    RelatedPasswordEntry(
                        linkId = link.id,
                        targetEntryId = target.id,
                        title = target.title,
                        username = target.username,
                    )
                }
            val otherRelatedIds = resolveOtherRelatedEntryIds(entry.id, links, relatedPasswords.mapTo(hashSetOf()) { it.targetEntryId })

            return EntryRelationsSnapshot(
                accountGroupEntries = accountGroupIds.mapNotNull(entriesById::get),
                otherRelatedEntries = otherRelatedIds.mapNotNull(entriesById::get),
                relatedPasswords = relatedPasswords,
            )
        }
    }

internal fun resolveAccountGroupEntryIds(
    entryId: EntryId,
    entryType: EntryType,
    links: List<EntryLink>,
): Set<EntryId> {
    val graph = EntryAccountGraph(links)
    val accountId =
        if (entryType == EntryType.ACCOUNT) {
            entryId
        } else {
            graph.accountFor(entryId)
        }

    return buildSet {
        if (accountId != null) {
            add(accountId)
            addAll(graph.membersOf(accountId))
        }
        remove(entryId)
    }
}

internal fun resolveDirectPasswordLinks(
    source: Entry,
    links: List<EntryLink>,
): List<EntryLink> {
    if (source.type != EntryType.LOGIN || source.deletedAt != null) return emptyList()
    return links.filter { link ->
        link.relationType == EntryRelationType.RELATED_TO && link.otherEndpoint(source.id) != null
    }
}

internal fun resolveOtherRelatedEntryIds(
    entryId: EntryId,
    links: List<EntryLink>,
    eligiblePasswordEntryIds: Set<EntryId>,
): List<EntryId> =
    resolveDirectRelatedEntryIds(entryId, links)
        .filterNot(eligiblePasswordEntryIds::contains)

private fun resolveDirectRelatedEntryIds(
    entryId: EntryId,
    links: List<EntryLink>,
): List<EntryId> =
    links
        .asSequence()
        .filter { it.relationType == EntryRelationType.RELATED_TO }
        .mapNotNull { it.otherEndpoint(entryId) }
        .distinct()
        .toList()

private fun EntryLink.otherEndpoint(entryId: EntryId): EntryId? =
    when (entryId) {
        sourceEntryId -> targetEntryId
        targetEntryId -> sourceEntryId
        else -> null
    }
