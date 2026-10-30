package com.aozijx.passly.feature.vault.presentation.editor.password

import com.aozijx.passly.feature.vault.presentation.editor.parseRawTags

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.model.Entry

fun AddPasswordFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.LOGIN]
    var draft = EntryDraft(EntryType.LOGIN)
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(password))

    draft = draft.withOptionalText(definition, FieldKey.USERNAME, username)
    draft = draft.withOptionalText(definition, FieldKey.PRIMARY_URL, websiteUrl)
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    val applicationIds = listOfNotNull(associatedAppPackage?.takeIf(String::isNotBlank)) +
        preservedAssociatedAppPackages.filterNot { it == associatedAppPackage }
    if (applicationIds.isNotEmpty()) {
        draft = draft.withValue(
            definition,
            FieldKey.APPLICATION_IDS,
            EntryDraftValue.TextList(applicationIds.toList()),
        )
    }
    return draft.withTags(definition, tags)
}

internal fun AddPasswordFormState.fromEntry(entry: Entry): AddPasswordFormState = copy(
    title = entry.title,
    username = entry.username,
    password = entry.secret.login?.password.orEmpty(),
    websiteUrl = entry.associations.primaryUrl.orEmpty(),
    notes = entry.secret.notes.orEmpty(),
    tags = entry.tags.joinToString(", "),
    associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
    preservedAssociatedAppPackages = entry.associations.applicationIds.drop(1).toSet(),
    isPasswordVisible = false,
)

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
    val values = parseRawTags(rawTags)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
