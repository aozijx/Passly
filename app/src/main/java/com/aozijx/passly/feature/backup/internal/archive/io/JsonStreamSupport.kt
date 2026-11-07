package com.aozijx.passly.feature.backup.internal.archive.io

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken

/** Copies one bounded JSON value and leaves the parser on its final token. */
internal fun JsonParser.copyCurrentValueTo(generator: JsonGenerator) {
    var depth = 0
    var token = currentToken ?: error("JSON 值缺少起始 token")
    do {
        generator.copyCurrentEvent(this)
        when (token) {
            JsonToken.START_OBJECT, JsonToken.START_ARRAY -> depth++
            JsonToken.END_OBJECT, JsonToken.END_ARRAY -> depth--
            else -> Unit
        }
        if (depth == 0) return
        token = nextToken() ?: error("JSON 值未闭合")
    } while (true)
}

/** Visits direct object fields with the parser positioned on each field value. */
internal inline fun JsonParser.forEachObjectField(action: (name: String) -> Unit) {
    require(currentToken == JsonToken.START_OBJECT) { "JSON 对象无效" }
    while (nextToken() == JsonToken.FIELD_NAME) {
        val name = currentName()
        require(nextToken() != null) { "JSON 字段缺少值: $name" }
        action(name)
    }
    require(currentToken == JsonToken.END_OBJECT) { "JSON 对象未闭合" }
}
