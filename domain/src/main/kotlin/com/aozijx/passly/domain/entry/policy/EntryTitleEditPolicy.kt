package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType

sealed interface EntryTitleEditDecision {
    data object Unchanged : EntryTitleEditDecision

    data class Change(val edit: EntryEdit) : EntryTitleEditDecision
}

/** The domain value edited by the Detail title field for this entry type. */
val Entry.titleEditValue: String
    get() =
        if (type == EntryType.WIFI) {
            secret.wifi?.ssid?.takeIf(String::isNotBlank) ?: title
        } else {
            title
        }

/** Applies the entry-type title rules before a Detail title edit reaches persistence. */
fun Entry.planTitleEdit(draft: String): EntryTitleEditDecision {
    val value = if (type == EntryType.WIFI) draft else draft.trim()

    if (value.isBlank() || value == titleEditValue) return EntryTitleEditDecision.Unchanged

    val edit = if (type == EntryType.WIFI) EntryEdit.SetWifiNetworkName(value) else EntryEdit.SetTitle(value)
    return EntryTitleEditDecision.Change(edit)
}
