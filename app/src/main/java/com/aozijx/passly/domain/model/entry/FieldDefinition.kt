package com.aozijx.passly.domain.model.entry

/**
 * 字段定义，描述详情页面中单个字段的元信息。
 */
data class FieldDefinition(
    val key: String,
    val label: String,
    val isRequired: Boolean = false,
    val isSensitive: Boolean = false,
    val fieldType: FieldType = FieldType.TEXT
)
