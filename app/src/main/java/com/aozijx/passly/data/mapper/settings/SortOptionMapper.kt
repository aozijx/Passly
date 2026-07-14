package com.aozijx.passly.data.mapper.settings

import com.aozijx.passly.R
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.settings.SortOption

fun SortOption.labelResId(): Int = when (this) {
    SortOption.DEFAULT -> R.string.sort_default
    SortOption.NAME_ASC, SortOption.NAME_DESC -> R.string.sort_name
    SortOption.CREATED_DESC, SortOption.CREATED_ASC -> R.string.sort_created
    SortOption.UPDATED_DESC, SortOption.UPDATED_ASC -> R.string.sort_updated
    SortOption.USAGE_DESC, SortOption.USAGE_ASC -> R.string.sort_usage
}

fun SortOption.apply(items: List<VaultEntry>): List<VaultEntry> {
    return when (this) {
        SortOption.DEFAULT -> items.sortedWith(
            compareByDescending<VaultEntry> { it.favorite }
                .thenByDescending { it.usageCount }
                .thenByDescending { it.createdAt }
        )

        SortOption.NAME_ASC -> items.sortedBy { it.title.lowercase() }
        SortOption.NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
        SortOption.CREATED_DESC -> items.sortedByDescending { it.createdAt }
        SortOption.CREATED_ASC -> items.sortedBy { it.createdAt }
        SortOption.UPDATED_DESC -> items.sortedByDescending { it.updatedAt }
        SortOption.UPDATED_ASC -> items.sortedBy { it.updatedAt }
        SortOption.USAGE_DESC -> items.sortedByDescending { it.usageCount }
        SortOption.USAGE_ASC -> items.sortedBy { it.usageCount }
    }
}
