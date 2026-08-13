package com.aozijx.passly.domain.entry.model.credential

data class CustomField(
    val name: String,
    val value: String,
    val kind: CustomFieldKind = CustomFieldKind.TEXT,
) {
    init {
        require(name.isNotBlank()) { "Custom field name cannot be blank" }
    }
}

enum class CustomFieldKind { TEXT, HIDDEN }
