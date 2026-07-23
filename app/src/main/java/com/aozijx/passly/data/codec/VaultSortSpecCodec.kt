package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.model.settings.SortDirection
import com.aozijx.passly.domain.model.settings.VaultSortField
import com.aozijx.passly.domain.model.settings.VaultSortSpec

/**
 * [VaultSortSpec] 序列化/反序列化工具。
 *
 * 序列化格式：`"FIELD:DIRECTION:PIN_FAVORITES:TIE_BREAKER"`
 * 示例：`"LAST_USED_AT:DESC:true:ID"`
 *
 * 从 Domain 模型移出，避免 Domain 依赖序列化格式。
 */
object VaultSortSpecCodec {

    private fun tryParseField(name: String): VaultSortField? =
        VaultSortField.entries.find { it.name == name }

    private fun tryParseDirection(name: String): SortDirection? =
        SortDirection.entries.find { it.name == name }

    fun serialize(spec: VaultSortSpec): String {
        if (spec == VaultSortSpec.DEFAULT) return "DEFAULT"
        return "${spec.field.name}:${spec.direction.name}:${spec.pinFavorites}:${spec.tieBreaker.name}"
    }

    fun parse(value: String): VaultSortSpec {
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
        return VaultSortSpec.DEFAULT
    }
}
