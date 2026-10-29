package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object SshKeyEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.SSH_KEY,
        listOf(
            titleField(),
            secret(FieldKey.USERNAME),
            high(FieldKey.SSH_KEY, SensitiveFieldKey.SSH_PRIVATE_KEY, required = true),
            secret(FieldKey.SSH_PUBLIC_KEY),
            high(FieldKey.SSH_PASSPHRASE, SensitiveFieldKey.SSH_PASSPHRASE),
            field(FieldKey.PRIMARY_URL),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
