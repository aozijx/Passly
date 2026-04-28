package com.aozijx.passly.data.mapper

import com.aozijx.passly.core.designsystem.model.VaultCardStyle

/**
 * 设置相关的映射逻辑，将存储格式与业务/UI模型解耦
 */
object SettingsMapper {
    private const val VISIBLE_VAULT_TABS_SEPARATOR = ","
    private const val CARD_STYLE_MAP_SEPARATOR = ";"
    private const val CARD_STYLE_ENTRY_SEPARATOR = ":"

    fun decodeVisibleTabs(raw: String?): Set<String>? {
        if (raw == null) return null
        return raw.split(VISIBLE_VAULT_TABS_SEPARATOR)
            .mapNotNull { it.trim().takeIf { t -> t.isNotEmpty() } }
            .toSet()
    }

    fun parseCardStyleMap(raw: String?): Map<Int, VaultCardStyle> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(CARD_STYLE_MAP_SEPARATOR).mapNotNull { token ->
            val parts = token.split(CARD_STYLE_ENTRY_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val key = parts[0].toIntOrNull() ?: return@mapNotNull null
            key to VaultCardStyle.fromKey(parts[1])
        }.toMap()
    }

    fun encodeCardStyleMap(map: Map<Int, VaultCardStyle>): String {
        return map.entries.sortedBy { it.key }
            .joinToString(CARD_STYLE_MAP_SEPARATOR) { "${it.key}:${it.value.key}" }
    }
}