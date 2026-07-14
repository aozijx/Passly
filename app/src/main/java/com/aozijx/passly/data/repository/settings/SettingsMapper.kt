package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.domain.model.settings.VaultCardStyle

object SettingsMapper {

    const val DEFAULT_STYLE_KEY = -1

    fun parseCardStyleMap(encoded: String?): Map<Int, VaultCardStyle> {
        if (encoded.isNullOrEmpty()) return emptyMap()
        return encoded.split(";").filter { it.isNotEmpty() }
            .associate { pair ->
                val parts = pair.split(":")
                if (parts.size >= 2) {
                    val key = parts[0].toIntOrNull() ?: DEFAULT_STYLE_KEY
                    key to VaultCardStyle.fromKey(parts[1])
                } else {
                    DEFAULT_STYLE_KEY to VaultCardStyle.DEFAULT
                }
            }
    }

    fun encodeCardStyleMap(map: Map<Int, VaultCardStyle>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value.key}" }
    }

    fun decodeVisibleTabs(encoded: String?): Set<String>? {
        if (encoded.isNullOrEmpty()) return null
        return encoded.split(",").filter { it.isNotEmpty() }.toSet()
    }
}