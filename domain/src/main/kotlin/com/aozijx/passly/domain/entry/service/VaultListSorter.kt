package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.VaultSortField
import com.aozijx.passly.domain.settings.model.VaultSortSpec

/** 对 Domain 列表投影执行稳定排序。 */
object VaultListSorter {

    fun sort(items: List<EntryListItem>, spec: VaultSortSpec): List<EntryListItem> {
        val primaryComparator: Comparator<EntryListItem> = when (spec.field) {
            VaultSortField.TITLE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.title.lowercase() } else compareByDescending { it.title.lowercase() }

            VaultSortField.CREATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.createdAt } else compareByDescending { it.createdAt }

            VaultSortField.UPDATED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy { it.updatedAt } else compareByDescending { it.updatedAt }

            VaultSortField.LAST_USED_AT -> if (spec.direction == SortDirection.ASC)
                compareBy(nullsLast()) { it.lastUsedAt }
            else compareByDescending(nullsLast()) { it.lastUsedAt }

            VaultSortField.USAGE_FREQUENCY -> if (spec.direction == SortDirection.ASC)
                compareBy { it.usageCount } else compareByDescending { it.usageCount }

            VaultSortField.ENTRY_TYPE -> if (spec.direction == SortDirection.ASC)
                compareBy { it.entryType } else compareByDescending { it.entryType }

            VaultSortField.ID -> if (spec.direction == SortDirection.ASC)
                compareBy { it.id } else compareByDescending { it.id }
        }

        val tieBreakerComparator: Comparator<EntryListItem> = when (spec.tieBreaker) {
            VaultSortField.ID -> compareBy { it.id }
            VaultSortField.CREATED_AT -> compareByDescending { it.createdAt }
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
