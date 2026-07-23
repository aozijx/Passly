package com.aozijx.passly.domain.model.entry

import kotlinx.serialization.Serializable

@Serializable
data class VaultMetadata(
    val entryId: String,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val icon: String?,
    val iconCustomPath: String? = null,
    val website: WebsiteInfo? = null,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val expiresAt: Long? = null
)
