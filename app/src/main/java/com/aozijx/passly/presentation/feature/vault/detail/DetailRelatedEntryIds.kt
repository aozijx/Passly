package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph

internal object DetailRelatedEntryIds {
    fun resolve(
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
}
