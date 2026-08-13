package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection

/** 对 Domain 列表投影执行稳定排序。 */
object EntryListSorter {

    fun sort(items: List<EntryListItem>, spec: EntrySort): List<EntryListItem> {
        val primaryComparator: Comparator<EntryListItem> = when (spec.field) {
            EntrySortField.TITLE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.title.lowercase() } else compareByDescending { it.title.lowercase() }

            EntrySortField.CREATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.createdAt } else compareByDescending { it.createdAt }

            EntrySortField.UPDATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.updatedAt } else compareByDescending { it.updatedAt }

            EntrySortField.LAST_USED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy(nullsLast()) { it.lastUsedAt }
            else compareByDescending(nullsLast()) { it.lastUsedAt }

            EntrySortField.USAGE_FREQUENCY -> if (spec.direction == SortDirection.ASC)
                compareBy { it.usageCount } else compareByDescending { it.usageCount }

            EntrySortField.ENTRY_TYPE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.entryType } else compareByDescending { it.entryType }

            EntrySortField.ID -> if (spec.direction == SortDirection.ASC)
                compareBy { it.id } else compareByDescending { it.id }
        }

        val tieBreakerComparator: Comparator<EntryListItem> = when (spec.tieBreaker) {
            EntrySortField.ID -> compareBy { it.id }
            EntrySortField.CREATED_AT -> compareByDescending { it.createdAt }
            else -> compareByDescending { it.createdAt }
        }

        return items.sortedWith(
            if (spec.pinFavorites) {
                compareByDescending<EntryListItem> { it.favorite }
                    .then(primaryComparator)
                    .then(tieBreakerComparator)
            } else {
                primaryComparator.then(tieBreakerComparator)
            }
        )
    }
}
