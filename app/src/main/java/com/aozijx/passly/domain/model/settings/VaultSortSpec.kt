package com.aozijx.passly.domain.model.settings

import com.aozijx.passly.domain.model.lookup.VaultListItem

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

        @JvmStatic
        fun serialize(spec: VaultSortSpec): String {
            if (spec == DEFAULT) return "DEFAULT"
            return "${spec.field.name}:${spec.direction.name}:${spec.pinFavorites}:${spec.tieBreaker.name}"
        }

        @JvmStatic
        fun parse(value: String): VaultSortSpec {
            // 向后兼容：尝试解析旧版 SortOption.name
            val oldOption = SortOption.entries.find { it.name == value }
            if (oldOption != null) return oldOption.toSpec()

            val parts = value.split(':')
            if (parts.size == 4) {
                val field = tryParseField(parts[0])
                val direction = tryParseDirection(parts[1])
                val pinFav = parts[2].toBoolean()
                val tie = tryParseField(parts[3])
                if (field != null && direction != null) {
                    return VaultSortSpec(field, direction, pinFav, tie ?: VaultSortField.ID)
                }
            }
            return DEFAULT
        }

        private fun tryParseField(name: String): VaultSortField? =
            VaultSortField.entries.find { it.name == name }

        private fun tryParseDirection(name: String): SortDirection? =
            SortDirection.entries.find { it.name == name }
    }

    fun apply(items: List<VaultListItem>): List<VaultListItem> {
        val primaryComparator: Comparator<VaultListItem> = when (field) {
            VaultSortField.TITLE -> if (direction == SortDirection.ASC)
                compareBy { it.title.lowercase() } else compareByDescending { it.title.lowercase() }

            VaultSortField.CREATED_AT -> if (direction == SortDirection.ASC)
                compareBy { it.createdAt } else compareByDescending { it.createdAt }

            VaultSortField.UPDATED_AT -> if (direction == SortDirection.ASC)
                compareBy { it.updatedAt } else compareByDescending { it.updatedAt }

            VaultSortField.LAST_USED_AT -> if (direction == SortDirection.ASC)
                compareBy(nullsLast()) { it.lastUsedAt } else compareByDescending(nullsLast()) { it.lastUsedAt }

            VaultSortField.USAGE_FREQUENCY -> if (direction == SortDirection.ASC)
                compareBy { it.usageCount } else compareByDescending { it.usageCount }

            VaultSortField.ENTRY_TYPE -> if (direction == SortDirection.ASC)
                compareBy { it.entryType } else compareByDescending { it.entryType }

            VaultSortField.ID -> if (direction == SortDirection.ASC)
                compareBy { it.id } else compareByDescending { it.id }
        }

        val tieBreakerComparator: Comparator<VaultListItem> = when (tieBreaker) {
            VaultSortField.ID -> compareBy { it.id }
            VaultSortField.CREATED_AT -> compareByDescending { it.createdAt }
            else -> compareByDescending { it.createdAt }
        }

        return items.sortedWith(
            if (pinFavorites) {
                compareByDescending<VaultListItem> { it.favorite }
                    .then(primaryComparator)
                    .then(tieBreakerComparator)
            } else {
                primaryComparator.then(tieBreakerComparator)
            }
        )
    }
}
