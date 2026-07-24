package com.aozijx.passly.domain.entry.model.secret

data class CustomField(
    val name: String,
    val value: String,
    val type: Int = 0
)
