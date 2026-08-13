package com.aozijx.passly.data.settings.model

enum class LibrarySortField {
    TITLE,
    CREATED_AT,
    UPDATED_AT,
    LAST_USED_AT,
    USAGE_FREQUENCY,
    ENTRY_TYPE,
    ID
}

enum class SortDirection { ASC, DESC }

/**
 * 排序规格，替代原先的 [SortOption] 组合枚举。
 *
 * 序列化格式：`"FIELD:DIRECTION:PIN_FAVORITES:TIE_BREAKER"`
 * 示例：`"LAST_USED_AT:DESC:true:ID"`
 */
data class LibrarySortSpec(
    val field: LibrarySortField,
    val direction: SortDirection,
    val pinFavorites: Boolean = true,
    val tieBreaker: LibrarySortField = LibrarySortField.ID
) {
    fun toggled(): LibrarySortSpec = copy(
        direction = if (direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
    )

    companion object {
        /** 默认排序：收藏 → 最近使用 → 最近更新 → ID */
        val DEFAULT = LibrarySortSpec(
            field = LibrarySortField.LAST_USED_AT,
            direction = SortDirection.DESC,
            pinFavorites = true,
            tieBreaker = LibrarySortField.ID
        )

        /** 预设选项（用于 UI 菜单）。 */
        fun presets(): List<LibrarySortSpec> = listOf(
            DEFAULT,
            LibrarySortSpec(LibrarySortField.TITLE, SortDirection.ASC),
            LibrarySortSpec(LibrarySortField.CREATED_AT, SortDirection.DESC),
            LibrarySortSpec(LibrarySortField.UPDATED_AT, SortDirection.DESC),
            LibrarySortSpec(LibrarySortField.USAGE_FREQUENCY, SortDirection.DESC)
        )
    }
}
