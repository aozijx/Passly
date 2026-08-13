package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability

private val orderedCapabilities = listOf(
    EntryCapability.PASSWORD,
    EntryCapability.OTP,
    EntryCapability.SSH_KEY,
    EntryCapability.PASSKEY,
    EntryCapability.WIFI,
    EntryCapability.IDENTITY,
    EntryCapability.CARD,
    EntryCapability.ATTACHMENTS,
    EntryCapability.CUSTOM_FIELDS,
)

internal fun EntryCapabilities.toDatabaseFlags(): Int =
    orderedCapabilities.foldIndexed(0) { index, flags, capability ->
        if (capability in this) flags or (1 shl index) else flags
    }

internal fun Int.toEntryCapabilities(): EntryCapabilities =
    EntryCapabilities(
        orderedCapabilities.filterIndexedTo(mutableSetOf()) { index, _ ->
            this and (1 shl index) != 0
        }
    )

internal fun Int.hasEntryCapability(capability: EntryCapability): Boolean {
    val index = orderedCapabilities.indexOf(capability)
    return index >= 0 && this and (1 shl index) != 0
}

internal fun databaseFlag(capability: EntryCapability): Int =
    1 shl orderedCapabilities.indexOf(capability)
