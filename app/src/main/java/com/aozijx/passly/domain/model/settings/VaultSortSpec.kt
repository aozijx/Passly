package com.aozijx.passly.domain.model.settings

enum class VaultSortField {
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
data class VaultSortSpec(
    val field: VaultSortField,
    val direction: SortDirection,
    val pinFavorites: Boolean = true,
    val tieBreaker: VaultSortField = VaultSortField.ID
) {
    fun toggled(): VaultSortSpec = copy(
        direction = if (direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
    )

    companion object {
        /** 默认排序：收藏 → 最近使用 → 最近更新 → ID */
        val DEFAULT = VaultSortSpec(
            field = VaultSortField.LAST_USED_AT,
            direction = SortDirection.DESC,
            pinFavorites = true,
            tieBreaker = VaultSortField.ID
        )

        /** 预设选项（用于 UI 菜单）。 */
        fun presets(): List<VaultSortSpec> = listOf(
            DEFAULT,
            VaultSortSpec(VaultSortField.TITLE, SortDirection.ASC),
            VaultSortSpec(VaultSortField.CREATED_AT, SortDirection.DESC),
            VaultSortSpec(VaultSortField.UPDATED_AT, SortDirection.DESC),
            VaultSortSpec(VaultSortField.LAST_USED_AT, SortDirection.DESC),
            VaultSortSpec(VaultSortField.USAGE_FREQUENCY, SortDirection.DESC)
        )
    }
}
