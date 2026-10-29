package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey

internal object NoteEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.NOTE,
        listOf(titleField(), secret(FieldKey.NOTES), field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST)),
    )
}
