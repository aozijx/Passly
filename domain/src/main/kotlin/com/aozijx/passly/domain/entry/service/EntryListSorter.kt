package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.LibrarySortField
import com.aozijx.passly.domain.settings.model.LibrarySortSpec

/** 对 Domain 列表投影执行稳定排序。 */
object EntryListSorter {

    fun sort(items: List<EntryListItem>, spec: LibrarySortSpec): List<EntryListItem> {
        val primaryComparator: Comparator<EntryListItem> = when (spec.field) {
            LibrarySortField.TITLE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.title.lowercase() } else compareByDescending { it.title.lowercase() }

            LibrarySortField.CREATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.createdAt } else compareByDescending { it.createdAt }

            LibrarySortField.UPDATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.updatedAt } else compareByDescending { it.updatedAt }

            LibrarySortField.LAST_USED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy(nullsLast()) { it.lastUsedAt }
            else compareByDescending(nullsLast()) { it.lastUsedAt }

            LibrarySortField.USAGE_FREQUENCY -> if (spec.direction == SortDirection.ASC)
                compareBy { it.usageCount } else compareByDescending { it.usageCount }

            LibrarySortField.ENTRY_TYPE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.entryType } else compareByDescending { it.entryType }

            LibrarySortField.ID -> if (spec.direction == SortDirection.ASC)
                compareBy { it.id } else compareByDescending { it.id }
        }

        val tieBreakerComparator: Comparator<EntryListItem> = when (spec.tieBreaker) {
            LibrarySortField.ID -> compareBy { it.id }
            LibrarySortField.CREATED_AT -> compareByDescending { it.createdAt }
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
