package com.aozijx.passly.features.vault.model

import com.aozijx.passly.R
import com.aozijx.passly.domain.model.VaultSummary

enum class SortOption(val labelResId: Int, val group: SortGroup) {
    DEFAULT(R.string.sort_default, SortGroup.STANDALONE),
    NAME_ASC(R.string.sort_name, SortGroup.NAME),
    NAME_DESC(R.string.sort_name, SortGroup.NAME),
    CREATED_DESC(R.string.sort_created, SortGroup.CREATED),
    CREATED_ASC(R.string.sort_created, SortGroup.CREATED),
    UPDATED_DESC(R.string.sort_updated, SortGroup.UPDATED),
    UPDATED_ASC(R.string.sort_updated, SortGroup.UPDATED),
    USAGE_DESC(R.string.sort_usage, SortGroup.USAGE),
    USAGE_ASC(R.string.sort_usage, SortGroup.USAGE);

    enum class SortGroup { STANDALONE, NAME, CREATED, UPDATED, USAGE }

    fun apply(items: List<VaultSummary>): List<VaultSummary> {
        return when (this) {
            DEFAULT -> items.sortedWith(
                compareByDescending<VaultSummary> { it.favorite }
                    .thenByDescending { it.usageCount }
                    .thenByDescending { it.createdAt ?: 0L }
            )

            NAME_ASC -> items.sortedBy { it.title.lowercase() }
            NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
            CREATED_DESC -> items.sortedByDescending { it.createdAt ?: 0L }
            CREATED_ASC -> items.sortedBy { it.createdAt ?: 0L }
            UPDATED_DESC -> items.sortedByDescending { it.updatedAt ?: 0L }
            UPDATED_ASC -> items.sortedBy { it.updatedAt ?: 0L }
            USAGE_DESC -> items.sortedByDescending { it.usageCount }
            USAGE_ASC -> items.sortedBy { it.usageCount }
        }
    }

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