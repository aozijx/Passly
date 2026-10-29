package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object PasskeyEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.PASSKEY,
        listOf(
            titleField(),
            secret(FieldKey.PASSKEY_CREDENTIAL_ID),
            field(FieldKey.PASSKEY_RELYING_PARTY_ID),
            secret(FieldKey.PASSKEY_USER_HANDLE),
            high(FieldKey.PASSKEY_DATA, SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE, required = true),
            field(FieldKey.HARDWARE_INFO),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
