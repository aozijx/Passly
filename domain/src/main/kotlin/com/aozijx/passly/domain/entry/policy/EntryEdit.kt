package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.FieldKey

/** One logical aggregate edit, applied to the latest entry without reading or persisting data. */
sealed interface EntryEdit {
    data class SetTitle(val value: String) : EntryEdit {
        override fun toString() = "SetTitle(***)"
    }
    data class SetWifiNetworkName(val value: String) : EntryEdit {
        override fun toString() = "SetWifiNetworkName(***)"
    }
    data object ToggleFavorite : EntryEdit
    data class SetFieldValue(val fieldKey: FieldKey, val value: String) : EntryEdit {
        override fun toString() = "SetFieldValue($fieldKey, ***)"
    }
    data class SetNotes(val value: String?) : EntryEdit {
        override fun toString() = "SetNotes(***)"
    }
    data class SetPrimaryUrl(val value: String?) : EntryEdit {
        override fun toString() = "SetPrimaryUrl(***)"
    }
    data class SetApplicationIds(val values: Set<String>) : EntryEdit {
        override fun toString() = "SetApplicationIds(***)"
    }
    data class SetTags(val values: Set<String>) : EntryEdit {
        override fun toString() = "SetTags(***)"
    }
    data class SetIcon(val value: EntryIcon) : EntryEdit {
        override fun toString() = "SetIcon(***)"
    }
    data class ReplaceForm(val draft: EntryDraft) : EntryEdit {
        override fun toString() = "ReplaceForm(***)"
    }

    fun applyTo(entry: Entry): EntryEditResult {
        val updated = when (this) {
            is SetTitle -> {
                if (value.isBlank()) return invalid(FieldKey.TITLE, EntryViolationCode.REQUIRED)
                entry.copy(profile = entry.profile.copy(title = value))
            }
            is SetWifiNetworkName -> {
                val wifi = entry.secret.wifi ?: return EntryEditResult.Unsupported
                if (value.isBlank()) return invalid(FieldKey.WIFI_SSID, EntryViolationCode.REQUIRED)
                entry.copy(profile = entry.profile.copy(title = value),
                    secret = entry.secret.copy(credential = wifi.copy(ssid = value)))
            }
            ToggleFavorite -> entry.copy(profile = entry.profile.copy(favorite = !entry.favorite))
            is SetFieldValue -> return EntryFieldEditor.edit(entry, fieldKey, value)
            is SetNotes -> entry.copy(secret = entry.secret.copy(notes = value))
            is SetPrimaryUrl -> entry.copy(profile = entry.profile.copy(
                associations = entry.associations.copy(primaryUrl = value)))
            is SetApplicationIds -> {
                if (values.any(String::isBlank)) return invalid(FieldKey.APPLICATION_IDS)
                entry.copy(profile = entry.profile.copy(associations = entry.associations.copy(applicationIds = values)))
            }
            is SetTags -> {
                if (values.any(String::isBlank)) return invalid(FieldKey.TAGS)
                entry.copy(profile = entry.profile.copy(tags = values))
            }
            is SetIcon -> entry.copy(profile = entry.profile.copy(icon = value))
            is ReplaceForm -> return EntryFormEditor().apply(entry, draft)
        }
        return EntryEditResult.Updated(updated)
    }

    private fun invalid(field: FieldKey, code: EntryViolationCode = EntryViolationCode.INVALID_FORMAT) =
        EntryEditResult.Invalid(setOf(EntryViolation(field, code)))
}

sealed interface EntryEditResult {
    data class Updated(val entry: Entry) : EntryEditResult {
        override fun toString() = "Updated(***)"
    }
    data object Unsupported : EntryEditResult
    data class Invalid(val violations: Set<EntryViolation>) : EntryEditResult
}
