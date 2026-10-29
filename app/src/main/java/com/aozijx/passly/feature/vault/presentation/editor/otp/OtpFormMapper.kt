package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.feature.vault.presentation.editor.parseRawTags

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.otp.OtpConfigInputResult
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.otp.toNewEntryDraft

fun OtpFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.OTP]
    val parsed = configInput.parse()
    require(parsed is OtpConfigInputResult.Valid) { "OTP input must be valid before creating a draft" }
    val config = parsed.config
    var draft = config.toNewEntryDraft(title = title.trim())
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    return draft.withTags(definition, tags)
}

private fun EntryDraft.withOptionalText(
    definition: EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft = rawValue.trim().takeIf(String::isNotEmpty)
    ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
    ?: this

private fun EntryDraft.withTags(definition: EntryTypeDefinition, rawTags: String): EntryDraft {
    val values = parseRawTags(rawTags)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
