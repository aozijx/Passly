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
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding

fun OtpFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.OTP]
    val parsed = configInput.parse()
    require(parsed is OtpConfigInputResult.Valid) { "OTP input must be valid before creating a draft" }
    val config = parsed.config
    var draft = config.toNewEntryDraft(title = title.trim())
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    return draft.withTags(definition, tags)
}

internal fun OtpFormState.fromEntry(entry: Entry): OtpFormState {
    val config = entry.secret.otp?.config
    return copy(
        title = entry.title,
        issuer = config?.issuer.orEmpty(),
        accountName = config?.accountName.orEmpty(),
        secret = config?.secret.orEmpty(),
        period = config?.periodSeconds?.toString() ?: "30",
        digits = config?.digits?.toString() ?: "6",
        type = config?.type ?: OtpType.TOTP,
        algorithm = config?.algorithm ?: OtpHashAlgorithm.SHA1,
        encoding = config?.encoding ?: OtpSecretEncoding.BASE32,
        counter = config?.counter?.toString() ?: "0",
        uriText = "",
        notes = entry.secret.notes.orEmpty(),
        tags = entry.tags.joinToString(", "),
    )
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
