package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class DetailEntryData(
    val entry: Entry,
    val sensitiveFieldKeys: Set<SensitiveFieldKey>,
)

data class DetailRelatedPassword(
    val linkId: EntryLinkId,
    val targetEntryId: EntryId,
    val title: String,
    val username: String,
)

internal data class DetailSessionData(
    val detail: DetailEntryData,
    val accountGroupEntries: List<Entry>,
    val otherRelatedEntries: List<Entry>,
    val relatedPasswords: List<DetailRelatedPassword>,
)

internal data class DetailRelationsSnapshot(
    val accountGroupEntries: List<Entry>,
    val otherRelatedEntries: List<Entry>,
    val relatedPasswords: List<DetailRelatedPassword>,
)

internal class DetailSessionQuery
    @Inject
    constructor(
        private val entryQueryRepository: EntryQueryRepository,
        private val sensitiveFieldRepository: SensitiveFieldRepository,
        private val entryLinkRepository: EntryLinkRepository,
    ) {
        suspend fun open(entryId: EntryId): DetailSessionData? {
            val entry = entryQueryRepository.getById(entryId) ?: return null
            return coroutineScope {
                val detail = async { snapshot(entry) }
                val relations = async { loadRelations(entry) }
                val relationData = relations.await()
                DetailSessionData(
                    detail = detail.await(),
                    accountGroupEntries = relationData.accountGroupEntries,
                    otherRelatedEntries = relationData.otherRelatedEntries,
                    relatedPasswords = relationData.relatedPasswords,
                )
            }
        }

        suspend fun reloadRelations(entryId: EntryId): DetailRelationsSnapshot? {
            val entry = entryQueryRepository.getById(entryId) ?: return null
            return loadRelations(entry)
        }

        suspend fun snapshot(entry: Entry): DetailEntryData =
            DetailEntryData(
                entry = entry,
                sensitiveFieldKeys = sensitiveFieldRepository.getPresence(entry.id).keys,
            )

        private suspend fun loadRelations(entry: Entry): DetailRelationsSnapshot {
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
                    DetailRelatedPassword(
                        linkId = link.id,
                        targetEntryId = target.id,
                        title = target.title,
                        username = target.username,
                    )
                }
            val otherRelatedIds = resolveOtherRelatedEntryIds(entry.id, links, relatedPasswords.mapTo(hashSetOf()) { it.targetEntryId })

            return DetailRelationsSnapshot(
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
    links.asSequence()
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
