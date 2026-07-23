package com.aozijx.passly.data.model.payload.summary

import kotlinx.serialization.Serializable

@Serializable
data class WebsiteInfoPayload(
    val primaryUrl: String? = null,
    val matchDomains: Set<String> = emptySet(),
    val packageNames: Set<String> = emptySet()
)

@Serializable
data class SummaryPayload(
    val title: String,
    val username: String,
    val website: WebsiteInfoPayload? = null,
    val icon: String? = null,
    val iconCustomPath: String? = null,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val expiresAt: Long? = null
)
