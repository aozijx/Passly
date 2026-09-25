package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
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

internal class DetailSessionQuery @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val entryLinkRepository: EntryLinkRepository,
) {
    suspend fun open(entryId: EntryId): DetailSessionData? {
        val entry = entryQueryRepository.getById(entryId) ?: return null
        return coroutineScope {
            val detail = async { snapshot(entry) }
            val relatedEntries = async { relatedEntries(entry) }
            DetailSessionData(
                detail = detail.await(),
                relatedEntries = relatedEntries.await(),
            )
        }
    }

    suspend fun snapshot(entry: Entry): DetailEntryData = DetailEntryData(
        entry = entry,
        sensitiveFieldKeys = sensitiveFieldRepository.getPresence(entry.id).keys,
    )

    private suspend fun relatedEntries(entry: Entry): List<Entry> {
        val relatedIds = resolveRelatedEntryIds(
            entryId = entry.id,
            entryType = entry.type,
            links = entryLinkRepository.getAll(),
        )
        return buildList {
            relatedIds.forEach { relatedId ->
                entryQueryRepository.getById(relatedId)?.let(::add)
            }
        }
    }

}

internal fun resolveRelatedEntryIds(
    entryId: EntryId,
    entryType: EntryType,
    links: List<EntryLink>,
): Set<EntryId> {
    val graph = EntryAccountGraph(links)
    val accountId = if (entryType == EntryType.ACCOUNT) {
        entryId
    } else {
        graph.accountFor(entryId)
    } ?: return emptySet()

    return buildSet {
        add(accountId)
        addAll(graph.membersOf(accountId))
        remove(entryId)
    }
}
