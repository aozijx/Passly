package com.aozijx.passly.domain.model.entry.secret

data class CustomField(
    val name: String,
    val value: String,
    val type: Int = 0
)
