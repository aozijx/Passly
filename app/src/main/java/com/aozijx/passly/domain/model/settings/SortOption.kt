package com.aozijx.passly.domain.model.settings

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

/** 转换为新的 [VaultSortSpec] 结构。 */
fun SortOption.toSpec(): VaultSortSpec = when (this) {
    SortOption.DEFAULT -> VaultSortSpec.DEFAULT
    SortOption.NAME_ASC -> VaultSortSpec(VaultSortField.TITLE, SortDirection.ASC)
    SortOption.NAME_DESC -> VaultSortSpec(VaultSortField.TITLE, SortDirection.DESC)
    SortOption.CREATED_DESC -> VaultSortSpec(VaultSortField.CREATED_AT, SortDirection.DESC)
    SortOption.CREATED_ASC -> VaultSortSpec(VaultSortField.CREATED_AT, SortDirection.ASC)
    SortOption.UPDATED_DESC -> VaultSortSpec(VaultSortField.UPDATED_AT, SortDirection.DESC)
    SortOption.UPDATED_ASC -> VaultSortSpec(VaultSortField.UPDATED_AT, SortDirection.ASC)
    SortOption.USAGE_DESC -> VaultSortSpec(VaultSortField.USAGE_FREQUENCY, SortDirection.DESC)
    SortOption.USAGE_ASC -> VaultSortSpec(VaultSortField.USAGE_FREQUENCY, SortDirection.ASC)
}
