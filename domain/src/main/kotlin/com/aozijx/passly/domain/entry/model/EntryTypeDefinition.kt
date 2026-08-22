package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

/** Domain value shape. Presentation decides which concrete input component renders it. */
enum class EntryFieldValueType {
    TEXT,
    BOOLEAN,
    INTEGER,
    TEXT_LIST,
}

/** Disclosure boundary for a field after the vault itself has been unlocked. */
enum class EntryFieldAccess {
    SUMMARY,
    SECRET,
    HIGH_SENSITIVITY,
}

data class EntryFieldDefinition(
    val key: FieldKey,
    val valueType: EntryFieldValueType = EntryFieldValueType.TEXT,
    val required: Boolean = false,
    val access: EntryFieldAccess = EntryFieldAccess.SUMMARY,
    val sensitiveFieldKey: SensitiveFieldKey? = null,
) {
    init {
        require((access == EntryFieldAccess.HIGH_SENSITIVITY) == (sensitiveFieldKey != null)) {
            "High-sensitivity fields must have exactly one sensitive storage key"
        }
    }
}

/** Semantic field contract for one [EntryType], independent of labels and UI layout. */
data class EntryTypeDefinition(
    val type: EntryType,
    val fields: List<EntryFieldDefinition>,
) {
    init {
        require(fields.isNotEmpty()) { "Entry type definition cannot be empty" }
        require(fields.map(EntryFieldDefinition::key).distinct().size == fields.size) {
            "Entry type definition cannot contain duplicate field keys"
        }
        require(fields.count { it.key == FieldKey.TITLE && it.required } == 1) {
            "Every entry type must contain one required title field"
        }
        val sensitiveKeys = fields.mapNotNull(EntryFieldDefinition::sensitiveFieldKey)
        require(sensitiveKeys.distinct().size == sensitiveKeys.size) {
            "Entry type definition cannot map two fields to the same sensitive storage key"
        }
    }

    operator fun get(key: FieldKey): EntryFieldDefinition? = fields.firstOrNull { it.key == key }

    fun supports(key: FieldKey): Boolean = get(key) != null

    val requiredFields: Set<FieldKey>
        get() = fields
            .filter(EntryFieldDefinition::required)
            .mapTo(linkedSetOf(), EntryFieldDefinition::key)
}
