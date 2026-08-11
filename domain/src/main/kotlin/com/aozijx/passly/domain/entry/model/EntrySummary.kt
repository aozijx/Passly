package com.aozijx.passly.domain.entry.model

data class EntrySummary(
    val title: String,
    val username: String,
    val website: WebsiteInfo? = null,
    val icon: String? = null,
    val iconCustomPath: String? = null,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val expiresAt: Long? = null
)
