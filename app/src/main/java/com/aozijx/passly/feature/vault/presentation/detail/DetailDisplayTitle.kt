package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit

internal val Entry.detailDisplayTitle: String
    get() = if (type == EntryType.WIFI) {
        secret.wifi?.ssid?.takeIf(String::isNotBlank) ?: title
    } else {
        title
    }

internal sealed interface DetailTitleEditPlan {
    data object Cancel : DetailTitleEditPlan
    data class Save(val edit: EntryEdit) : DetailTitleEditPlan
}

internal fun Entry.planDetailTitleEdit(draft: String): DetailTitleEditPlan {
    val value = if (type == EntryType.WIFI) draft else draft.trim()
    if (value.isBlank() || value == detailDisplayTitle) return DetailTitleEditPlan.Cancel
    val edit = if (type == EntryType.WIFI) EntryEdit.SetWifiNetworkName(value) else EntryEdit.SetTitle(value)
    return DetailTitleEditPlan.Save(edit)
}
