package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryDetailData
import com.aozijx.passly.domain.entry.model.query.EntryDetailSnapshot
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryLinkKind
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
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
            val endpointIds = links.flatMapTo(linkedSetOf()) { listOf(it.firstEntryId, it.secondEntryId) }
            val entryTypes = entryQueryRepository.getTypesByIds(endpointIds.toList()) + (entry.id to entry.type)
            val accountGroupIds = resolveAccountGroupEntryIds(entry.id, entry.type, links, entryTypes)
            val directPasswordLinks = resolveDirectPasswordLinks(entry, links, entryTypes)
            val directRelatedIds = resolveDirectRelatedEntryIds(entry.id, links)
            val requestedEntryIds = (accountGroupIds + directRelatedIds).filterNot { it == entry.id }.distinct()
            val entriesById = entryQueryRepository.getByIds(requestedEntryIds).associateBy(Entry::id)
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
            val otherRelatedIds =
                resolveOtherRelatedEntryIds(
                    entry.id,
                    links,
                    (accountGroupIds + relatedPasswords.map { it.targetEntryId }).toSet(),
                )

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
    entryTypes: Map<EntryId, EntryType>,
): Set<EntryId> {
    val graph = EntryAccountGraph(links, entryTypes)
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
    entryTypes: Map<EntryId, EntryType>,
): List<EntryLink> {
    if (source.type != EntryType.LOGIN || source.deletedAt != null) return emptyList()
    return links.filter { link ->
        link.otherEndpoint(source.id)?.let { targetId ->
            entryTypes[targetId] == EntryType.LOGIN &&
                EntryLinkPolicy.classify(source.type, entryTypes.getValue(targetId)) == EntryLinkKind.RELATED_ENTRIES
        } == true
    }
}

internal fun resolveOtherRelatedEntryIds(
    entryId: EntryId,
    links: List<EntryLink>,
    excludedEntryIds: Set<EntryId>,
): List<EntryId> =
    resolveDirectRelatedEntryIds(entryId, links)
        .filterNot(excludedEntryIds::contains)

private fun resolveDirectRelatedEntryIds(
    entryId: EntryId,
    links: List<EntryLink>,
): List<EntryId> =
    links
        .asSequence()
        .mapNotNull { it.otherEndpoint(entryId) }
        .distinct()
        .toList()
