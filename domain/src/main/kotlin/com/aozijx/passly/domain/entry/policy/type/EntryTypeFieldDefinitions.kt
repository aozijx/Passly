package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun titleField() = field(FieldKey.TITLE, required = true)

internal fun field(
    key: FieldKey,
    valueType: EntryFieldValueType = EntryFieldValueType.TEXT,
    required: Boolean = false,
) = EntryFieldDefinition(key = key, valueType = valueType, required = required)

internal fun secret(
    key: FieldKey,
    required: Boolean = false,
) = EntryFieldDefinition(
    key = key,
    required = required,
    access = EntryFieldAccess.SECRET,
)

internal fun high(
    key: FieldKey,
    sensitiveFieldKey: SensitiveFieldKey,
    valueType: EntryFieldValueType = EntryFieldValueType.TEXT,
    required: Boolean = false,
) = EntryFieldDefinition(
    key = key,
    valueType = valueType,
    required = required,
    access = EntryFieldAccess.HIGH_SENSITIVITY,
    sensitiveFieldKey = sensitiveFieldKey,
)

internal fun loginFields() = listOf(
    titleField(),
    secret(FieldKey.USERNAME),
    secret(FieldKey.EMAIL),
    high(FieldKey.PASSWORD, SensitiveFieldKey.PASSWORD, required = true),
    field(FieldKey.PRIMARY_URL),
    field(FieldKey.DOMAINS, EntryFieldValueType.TEXT_LIST),
    field(FieldKey.APPLICATION_IDS, EntryFieldValueType.TEXT_LIST),
    secret(FieldKey.NOTES),
    field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
)

internal fun identityFields() = listOf(
    titleField(),
    high(FieldKey.ID_NUMBER, SensitiveFieldKey.IDENTITY_NUMBER, required = true),
    secret(FieldKey.SECURITY_QUESTION),
    secret(FieldKey.SECURITY_ANSWER),
    secret(FieldKey.NOTES),
    field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
)
