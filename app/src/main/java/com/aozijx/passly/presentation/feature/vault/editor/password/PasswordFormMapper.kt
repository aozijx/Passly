package com.aozijx.passly.presentation.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions

fun AddPasswordFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.LOGIN]
    var draft = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(password))

    draft = draft.withOptionalText(definition, FieldKey.USERNAME, username)
    draft = draft.withOptionalText(definition, FieldKey.PRIMARY_URL, website)
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    return draft.withTags(definition, tags)
}

private fun EntryDraft.withOptionalText(
    definition: com.aozijx.passly.domain.entry.model.EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft = rawValue.trim().takeIf(String::isNotEmpty)
    ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
    ?: this

private fun EntryDraft.withTags(
    definition: com.aozijx.passly.domain.entry.model.EntryTypeDefinition,
    rawTags: String,
): EntryDraft {
    val values = rawTags.split(',').map(String::trim).filter(String::isNotEmpty)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
