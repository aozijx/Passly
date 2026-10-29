package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object RecoveryCodeEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.RECOVERY_CODE,
        listOf(
            titleField(),
            high(
                FieldKey.RECOVERY_CODES,
                SensitiveFieldKey.RECOVERY_CODES,
                valueType = EntryFieldValueType.TEXT_LIST,
                required = true,
            ),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
