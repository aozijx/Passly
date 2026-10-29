package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey

internal object AccountEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.ACCOUNT,
        listOf(titleField(), field(FieldKey.USERNAME), field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST)),
    )
}
