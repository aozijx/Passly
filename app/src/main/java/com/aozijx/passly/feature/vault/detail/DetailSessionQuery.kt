package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
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

internal data class DetailSessionData(
    val detail: DetailEntryData,
    val relatedEntries: List<Entry>,
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
                val relatedEntries = async { loadRelatedEntries(entry) }
                DetailSessionData(
                    detail = detail.await(),
                    relatedEntries = relatedEntries.await(),
                )
            }
        }

        suspend fun reloadRelatedEntries(entryId: EntryId): List<Entry>? {
            val entry = entryQueryRepository.getById(entryId) ?: return null
            return loadRelatedEntries(entry)
        }

        suspend fun snapshot(entry: Entry): DetailEntryData =
            DetailEntryData(
                entry = entry,
                sensitiveFieldKeys = sensitiveFieldRepository.getPresence(entry.id).keys,
            )

        private suspend fun loadRelatedEntries(entry: Entry): List<Entry> {
            val relatedIds =
                resolveRelatedEntryIds(
                    entryId = entry.id,
                    entryType = entry.type,
                    links = entryLinkRepository.getAll(),
                )
            return entryQueryRepository.getByIds(relatedIds.toList())
        }
    }

internal fun resolveRelatedEntryIds(
    entryId: EntryId,
    entryType: EntryType,
    links: List<EntryLink>,
): Set<EntryId> {
    val directRelatedIds =
        links.asSequence()
            .filter { it.relationType == EntryRelationType.RELATED_TO }
            .mapNotNull { link ->
                when (entryId) {
                    link.sourceEntryId -> link.targetEntryId
                    link.targetEntryId -> link.sourceEntryId
                    else -> null
                }
            }.toSet()
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
        addAll(directRelatedIds)
        remove(entryId)
    }
}
