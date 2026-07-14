package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.CustomFieldPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toCustomFieldsPayload(): List<CustomFieldPayload> =
    customFields.map { CustomFieldPayload(it.name, it.value, it.type) }

fun VaultEntry.mergeCustomFields(payload: List<CustomFieldPayload>?): VaultEntry {
    payload ?: return this
    return copy(customFields = payload.map {
        CustomField(name = it.name, value = it.value, type = it.type)
    })
}