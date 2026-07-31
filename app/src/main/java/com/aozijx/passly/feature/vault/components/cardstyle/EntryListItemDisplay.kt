package com.aozijx.passly.feature.vault.components.cardstyle

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

internal val EntryListItem.userCategory: String?
    get() = tags.firstOrNull { it.isNotBlank() }?.trim()

internal val EntryListItem.categoryOrTemplateLabel: String
    get() = userCategory ?: entryType.displayName
