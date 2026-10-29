package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object SeedPhraseEntryDefinition {
    val definition = EntryTypeDefinition(
        EntryType.SEED_PHRASE,
        listOf(
            titleField(),
            high(FieldKey.SEED_PHRASE, SensitiveFieldKey.SEED_PHRASE, required = true),
            secret(FieldKey.NOTES),
            field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
        ),
    )
}
