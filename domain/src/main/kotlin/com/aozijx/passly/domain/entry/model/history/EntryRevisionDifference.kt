package com.aozijx.passly.domain.entry.model.history

import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

sealed interface RevisionFieldId {
    data object Title : RevisionFieldId
    data object Username : RevisionFieldId
    data object PrimaryUrl : RevisionFieldId
    data object Domains : RevisionFieldId
    data object ApplicationIds : RevisionFieldId
    data object Icon : RevisionFieldId
    data object Favorite : RevisionFieldId
    data object Tags : RevisionFieldId
    data object ExpiresAt : RevisionFieldId
    data object Credential : RevisionFieldId
    data object Notes : RevisionFieldId
    data object CustomFields : RevisionFieldId
    data object Links : RevisionFieldId
    data object Attachments : RevisionFieldId
    data class Sensitive(val key: SensitiveFieldKey) : RevisionFieldId
}

sealed interface RevisionFieldValue {
    data class Text(val value: String) : RevisionFieldValue
    data class TextSet(val values: Set<String>) : RevisionFieldValue
    data class Flag(val value: Boolean) : RevisionFieldValue
    data class EpochMillis(val value: Long) : RevisionFieldValue
    data class Icon(val value: EntryIcon) : RevisionFieldValue
    data class Credential(val value: EntryCredential) : RevisionFieldValue
    data class CustomFields(val values: List<CustomField>) : RevisionFieldValue
    data class Links(val values: List<EntryLink>) : RevisionFieldValue
    data class AttachmentIds(val values: Set<String>) : RevisionFieldValue
}

enum class RevisionDifferenceKind { ADDED, REMOVED, CHANGED, UNCHANGED }

data class EntryRevisionDifference(
    val field: RevisionFieldId,
    val kind: RevisionDifferenceKind,
    val before: RevisionFieldValue?,
    val after: RevisionFieldValue?,
) {
    init {
        if (field is RevisionFieldId.Sensitive) {
            require(before == null && after == null) {
                "Sensitive revision differences cannot contain values"
            }
        }
    }
}
