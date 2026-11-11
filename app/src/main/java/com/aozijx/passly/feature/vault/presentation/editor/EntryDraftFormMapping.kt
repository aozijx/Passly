package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey

internal fun EntryDraft.withOptionalText(
    definition: EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft =
    rawValue
        .trim()
        .takeIf(String::isNotEmpty)
        ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
        ?: this

internal fun EntryDraft.withTags(
    definition: EntryTypeDefinition,
    rawTags: String,
): EntryDraft {
    val values = parseRawTags(rawTags)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
