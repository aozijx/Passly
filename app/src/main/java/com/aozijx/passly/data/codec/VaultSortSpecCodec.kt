package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.LibrarySortField
import com.aozijx.passly.domain.settings.model.LibrarySortSpec

/**
 * [LibrarySortSpec] 序列化/反序列化工具。
 *
 * 序列化格式：`"FIELD:DIRECTION:PIN_FAVORITES:TIE_BREAKER"`
 * 示例：`"LAST_USED_AT:DESC:true:ID"`
 *
 * 从 Domain 模型移出，避免 Domain 依赖序列化格式。
 */
object LibrarySortSpecCodec {

    private fun tryParseField(name: String): LibrarySortField? =
        LibrarySortField.entries.find { it.name == name }

    private fun tryParseDirection(name: String): SortDirection? =
        SortDirection.entries.find { it.name == name }

    fun serialize(spec: LibrarySortSpec): String {
        if (spec == LibrarySortSpec.DEFAULT) return "DEFAULT"
        return "${spec.field.name}:${spec.direction.name}:${spec.pinFavorites}:${spec.tieBreaker.name}"
    }

    fun parse(value: String): LibrarySortSpec {
        val parts = value.split(':')
        if (parts.size == 4) {
            val field = tryParseField(parts[0])
            val direction = tryParseDirection(parts[1])
            val pinFav = parts[2].toBoolean()
            val tie = tryParseField(parts[3])
            if (field != null && direction != null) {
                return LibrarySortSpec(field, direction, pinFav, tie ?: LibrarySortField.ID)
            }
        }
        return LibrarySortSpec.DEFAULT
    }
}
