package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey

internal object WifiEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.WIFI,
        listOf(
            titleField(),
            field(FieldKey.WIFI_SSID, required = true),
            secret(FieldKey.PASSWORD),
            field(FieldKey.WIFI_SECURITY),
            field(FieldKey.WIFI_HIDDEN, EntryFieldValueType.BOOLEAN),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
