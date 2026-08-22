package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection

/** 对 Domain 列表投影执行稳定排序。 */
object EntryListSorter {

    fun sort(items: List<EntryListItem>, spec: EntrySort): List<EntryListItem> {
        val primary = comparator(spec.field, spec.direction)
        val tieBreaker = comparator(spec.tieBreaker, spec.tieBreaker.stableDirection)
        val stableId = comparator(EntrySortField.ID, SortDirection.ASC)
        val ordered = primary.then(tieBreaker).then(stableId)
        return items.sortedWith(
            if (spec.pinFavorites) {
                compareByDescending<EntryListItem> { it.favorite }.then(ordered)
            } else ordered
        )
    }

    private fun comparator(
        field: EntrySortField,
        direction: SortDirection,
    ): Comparator<EntryListItem> = Comparator { left, right ->
        when (field) {
            EntrySortField.TITLE -> compareValues(
                left.title.lowercase(),
                right.title.lowercase(),
                direction,
            )
            EntrySortField.CREATED_AT -> compareValues(left.createdAt, right.createdAt, direction)
            EntrySortField.UPDATED_AT -> compareValues(left.updatedAt, right.updatedAt, direction)
            EntrySortField.LAST_USED_AT -> compareValues(left.lastUsedAt, right.lastUsedAt, direction)
            EntrySortField.USAGE_FREQUENCY -> compareValues(
                left.usageCount,
                right.usageCount,
                direction,
            )
            EntrySortField.ENTRY_TYPE -> compareValues(left.entryType, right.entryType, direction)
            EntrySortField.ID -> compareValues(left.id.value, right.id.value, direction)
        }
    }

    /** Null is kept last regardless of direction. */
    private fun <T : Comparable<T>> compareValues(
        left: T?,
        right: T?,
        direction: SortDirection,
    ): Int = when {
        left == null && right == null -> 0
        left == null -> 1
        right == null -> -1
        direction == SortDirection.ASC -> left.compareTo(right)
        else -> right.compareTo(left)
    }
}
