package com.aozijx.passly.domain.entry.model

/** Identifies whether a draft creates a new aggregate or updates one optimistic-lock version. */
sealed interface EntryDraftTarget {
    val type: EntryType

    data class New(
        override val type: EntryType,
    ) : EntryDraftTarget

    data class Existing(
        val entryId: EntryId,
        override val type: EntryType,
        val expectedVersion: EntryVersion,
    ) : EntryDraftTarget
}

/** Typed values prevent data-driven editors from falling back to Map<String, Any>. */
sealed interface EntryDraftValue {
    val valueType: EntryFieldValueType
    val isEmpty: Boolean

    data class Text(val value: String) : EntryDraftValue {
        override val valueType: EntryFieldValueType = EntryFieldValueType.TEXT
        override val isEmpty: Boolean get() = value.isBlank()
    }

    data class TextList(val values: List<String>) : EntryDraftValue {
        override val valueType: EntryFieldValueType = EntryFieldValueType.TEXT_LIST
        override val isEmpty: Boolean get() = values.none { it.isNotBlank() }
    }

    data class Toggle(val enabled: Boolean) : EntryDraftValue {
        override val valueType: EntryFieldValueType = EntryFieldValueType.BOOLEAN
        override val isEmpty: Boolean = false
    }

    data class Number(val value: Int?) : EntryDraftValue {
        override val valueType: EntryFieldValueType = EntryFieldValueType.INTEGER
        override val isEmpty: Boolean get() = value == null
    }
}

/** Immutable domain draft shared by create and edit application flows. */
data class EntryDraft(
    val target: EntryDraftTarget,
    val values: Map<FieldKey, EntryDraftValue> = emptyMap(),
) {
    operator fun get(key: FieldKey): EntryDraftValue? = values[key]

    fun withValue(
        definition: EntryTypeDefinition,
        key: FieldKey,
        value: EntryDraftValue,
    ): EntryDraft {
        require(definition.type == target.type) {
            "Draft target type does not match its entry definition"
        }
        val field = requireNotNull(definition[key]) {
            "$key is not supported by ${definition.type}"
        }
        require(field.valueType == value.valueType) {
            "$key requires ${field.valueType}, not ${value.valueType}"
        }
        return copy(values = values + (key to value))
    }

    fun missingRequiredFields(definition: EntryTypeDefinition): Set<FieldKey> {
        require(definition.type == target.type) {
            "Draft target type does not match its entry definition"
        }
        return definition.requiredFields.filterTo(linkedSetOf()) { key ->
            values[key]?.isEmpty != false
        }
    }
}
