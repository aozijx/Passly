package com.aozijx.passly.domain.entry.model

/** Domain reference to an icon; resolving a resource or file belongs outside domain. */
data class EntryIcon(
    val name: String? = null,
    val customReference: String? = null,
    val color: String? = null,
)
