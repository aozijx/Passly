package com.aozijx.passly.domain.model.entry

/**
 * 字段分组，将相关字段组织在同一个标题下用于详情页展示。
 */
data class FieldGroup(
    val title: String,
    val fields: List<FieldDefinition>
)
