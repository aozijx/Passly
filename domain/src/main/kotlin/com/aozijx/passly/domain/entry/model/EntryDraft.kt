package com.aozijx.passly.domain.entry.model

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

    data class LongNumber(val value: Long?) : EntryDraftValue {
        override val valueType: EntryFieldValueType = EntryFieldValueType.LONG
        override val isEmpty: Boolean get() = value == null
    }
}

/** Immutable creation input; callers own identity generation and persistence. */
data class EntryDraft(
    val type: EntryType,
    val values: Map<FieldKey, EntryDraftValue> = emptyMap(),
) {
    operator fun get(key: FieldKey): EntryDraftValue? = values[key]

    fun withValue(
        definition: EntryTypeDefinition,
        key: FieldKey,
        value: EntryDraftValue,
    ): EntryDraft {
        require(definition.type == type) {
            "Draft type does not match its entry definition"
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
        require(definition.type == type) {
            "Draft type does not match its entry definition"
        }
        return definition.requiredFields.filterTo(linkedSetOf()) { key ->
            values[key]?.isEmpty != false
        }
    }

    /** Checks supplied values, including maps constructed without [withValue]. Omission is distinct from an empty number. */
    fun invalidProvidedFields(definition: EntryTypeDefinition): Set<FieldKey> {
        require(definition.type == type) {
            "Draft type does not match its entry definition"
        }
        return values.filterTo(linkedMapOf()) { (key, value) ->
            definition[key]?.valueType != value.valueType || when (value) {
                is EntryDraftValue.Number, is EntryDraftValue.LongNumber -> value.isEmpty
                is EntryDraftValue.Text, is EntryDraftValue.TextList, is EntryDraftValue.Toggle -> false
            }
        }.keys.toSet()
    }
}
