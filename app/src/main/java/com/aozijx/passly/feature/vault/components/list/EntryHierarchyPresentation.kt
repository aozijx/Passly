package com.aozijx.passly.feature.vault.components.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode

internal fun arrangeEntryHierarchy(
    entries: List<EntryListItem>,
    mode: EntryHierarchyDisplayMode
): List<EntryListItem> {
    if (entries.isEmpty()) return entries
    val ids = entries.mapTo(hashSetOf()) { it.id }
    return when (mode) {
        EntryHierarchyDisplayMode.COLLAPSED -> entries.filter {
            it.accountEntryId == null || it.accountEntryId !in ids
        }

        EntryHierarchyDisplayMode.SEPARATE -> entries.filter {
            it.entryType != EntryType.ACCOUNT
        }

        EntryHierarchyDisplayMode.EXPANDED -> {
            val children = entries
                .filter { it.accountEntryId != null }
                .groupBy { it.accountEntryId }
            buildList(entries.size) {
                val added = hashSetOf<String>()
                entries.filter {
                    it.accountEntryId == null || it.accountEntryId !in ids
                }.forEach { root ->
                    if (added.add(root.id)) add(root)
                    children[root.id].orEmpty().forEach { child ->
                        if (added.add(child.id)) add(child)
                    }
                }
                entries.forEach { if (added.add(it.id)) add(it) }
            }
        }
    }
}
