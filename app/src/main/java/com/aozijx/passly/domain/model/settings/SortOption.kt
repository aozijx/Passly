package com.aozijx.passly.domain.model.settings

import com.aozijx.passly.domain.model.entry.VaultEntry

enum class SortOption(val group: SortGroup) {
    DEFAULT(SortGroup.STANDALONE),
    NAME_ASC(SortGroup.NAME),
    NAME_DESC(SortGroup.NAME),
    CREATED_DESC(SortGroup.CREATED),
    CREATED_ASC(SortGroup.CREATED),
    UPDATED_DESC(SortGroup.UPDATED),
    UPDATED_ASC(SortGroup.UPDATED),
    USAGE_DESC(SortGroup.USAGE),
    USAGE_ASC(SortGroup.USAGE);

    enum class SortGroup { STANDALONE, NAME, CREATED, UPDATED, USAGE }

    val isDescending: Boolean
        get() = when (this) {
            DEFAULT -> false
            NAME_ASC -> false
            NAME_DESC -> true
            CREATED_DESC -> true
            CREATED_ASC -> false
            UPDATED_DESC -> true
            UPDATED_ASC -> false
            USAGE_DESC -> true
            USAGE_ASC -> false
        }

    fun toggled(): SortOption = when (this) {
        DEFAULT -> DEFAULT
        NAME_ASC -> NAME_DESC
        NAME_DESC -> NAME_ASC
        CREATED_DESC -> CREATED_ASC
        CREATED_ASC -> CREATED_DESC
        UPDATED_DESC -> UPDATED_ASC
        UPDATED_ASC -> UPDATED_DESC
        USAGE_DESC -> USAGE_ASC
        USAGE_ASC -> USAGE_DESC
    }

    companion object {
        fun displayOptions(): List<SortOption> = listOf(
            DEFAULT, NAME_ASC, CREATED_DESC, UPDATED_DESC, USAGE_DESC
        )
    }
}

fun SortOption.apply(items: List<VaultEntry>): List<VaultEntry> = when (this) {
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
