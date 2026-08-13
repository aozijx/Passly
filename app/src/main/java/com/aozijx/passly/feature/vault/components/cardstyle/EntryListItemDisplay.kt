package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.text.localizedName
import com.aozijx.passly.domain.entry.model.query.EntryListItem

internal val EntryListItem.userCategory: String?
    get() = tags.firstOrNull { it.isNotBlank() }?.trim()

@Composable
internal fun EntryListItem.categoryOrTemplateLabel(): String =
    userCategory ?: entryType.localizedName()
