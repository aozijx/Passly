package com.aozijx.passly.feature.backup.internal.archive.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupFieldRecord(
    val key: String,
    val value: BackupFieldValue,
)

@Serializable
data class BackupFieldValue(
    val kind: String,
    val text: String? = null,
    val texts: List<String>? = null,
    @SerialName("boolean") val booleanValue: Boolean? = null,
    @SerialName("integer") val integerValue: Int? = null,
    @SerialName("long") val longValue: Long? = null,
    val customFields: List<BackupCustomFieldValue>? = null,
) {
    companion object {
        const val KIND_TEXT = "text"
        const val KIND_TEXT_LIST = "text_list"
        const val KIND_BOOLEAN = "boolean"
        const val KIND_INTEGER = "integer"
        const val KIND_LONG = "long"
        const val KIND_CUSTOM_FIELDS = "custom_fields"

        fun text(value: String) = BackupFieldValue(kind = KIND_TEXT, text = value)
        fun texts(values: List<String>) = BackupFieldValue(kind = KIND_TEXT_LIST, texts = values)
        fun boolean(value: Boolean) = BackupFieldValue(kind = KIND_BOOLEAN, booleanValue = value)
        fun integer(value: Int) = BackupFieldValue(kind = KIND_INTEGER, integerValue = value)
        fun long(value: Long) = BackupFieldValue(kind = KIND_LONG, longValue = value)
        fun customFields(values: List<BackupCustomFieldValue>) = BackupFieldValue(
            kind = KIND_CUSTOM_FIELDS,
            customFields = values,
        )
    }
}

@Serializable
data class BackupCustomFieldValue(
    val name: String,
    val value: String,
    val kind: String,
) {
    companion object {
        const val KIND_TEXT = "text"
        const val KIND_HIDDEN = "hidden"
    }
}
