package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object OtpEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.OTP,
        listOf(
            titleField(),
            high(FieldKey.OTP_SECRET, SensitiveFieldKey.OTP_SECRET, required = true),
            field(FieldKey.OTP_ISSUER),
            field(FieldKey.OTP_ACCOUNT_NAME),
            field(FieldKey.OTP_TYPE),
            field(FieldKey.OTP_PERIOD, EntryFieldValueType.INTEGER),
            field(FieldKey.OTP_COUNTER, EntryFieldValueType.LONG),
            field(FieldKey.OTP_DIGITS, EntryFieldValueType.INTEGER),
            field(FieldKey.OTP_ALGORITHM),
            field(FieldKey.OTP_ENCODING),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
