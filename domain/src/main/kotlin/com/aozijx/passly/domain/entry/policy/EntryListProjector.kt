package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery

/** Applies in-memory query semantics to an unlocked entry summary snapshot. */
object EntryListProjector {

    fun project(items: List<EntryListItem>, query: EntryListQuery): List<EntryListItem> {
        val selected = items.asSequence()
            .filter { item -> item.matches(query.normalizedSearchText) }
            .filter { item -> item.matches(query.filter) }
            .filter { item -> item.matchesCategory(query.normalizedCategory) }
            .toList()
        return EntryListSorter.sort(selected, query.sort)
    }

    fun categories(items: List<EntryListItem>): List<String> = items.asSequence()
        .flatMap { item -> item.tags.asSequence() }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .toList()

    private fun EntryListItem.matches(searchText: String): Boolean {
        if (searchText.isEmpty()) return true
        return sequenceOf(title, username, associations.primaryUrl.orEmpty())
            .plus(tags.asSequence())
            .plus(associations.domains.asSequence())
            .plus(associations.applicationIds.asSequence())
            .any { candidate -> searchText in candidate.lowercase() }
    }

    private fun EntryListItem.matches(filter: EntryFilter): Boolean = when (filter) {
        EntryFilter.ALL -> true
        EntryFilter.PASSWORD_ONLY -> hasPassword
        EntryFilter.TOTP_ONLY -> hasOtp
    }

    private fun EntryListItem.matchesCategory(category: String?): Boolean =
        category == null || tags.any { tag -> tag.equals(category, ignoreCase = true) }
}
