package com.aozijx.passly.presentation.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions

fun OtpFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.OTP]
    val digitsValue = if (type == OtpType.STEAM) 5 else digits.trim().toIntOrNull() ?: 6
    val algorithmValue = if (type == OtpType.STEAM) {
        OtpHashAlgorithm.SHA1
    } else {
        OtpHashAlgorithm.entries.firstOrNull { it.name.equals(algorithm.trim(), ignoreCase = true) }
            ?: OtpHashAlgorithm.SHA1
    }
    var draft = EntryDraft(EntryDraftTarget.New(EntryType.OTP))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.OTP_SECRET, EntryDraftValue.Text(secret.trim()))
        .withValue(definition, FieldKey.OTP_TYPE, EntryDraftValue.Text(type.name))
        .withValue(definition, FieldKey.OTP_DIGITS, EntryDraftValue.Number(digitsValue))
        .withValue(definition, FieldKey.OTP_ALGORITHM, EntryDraftValue.Text(algorithmValue.name))
        .withValue(definition, FieldKey.OTP_ENCODING, EntryDraftValue.Text(encoding.name))

    draft = if (type == OtpType.HOTP) {
        draft.withValue(
            definition,
            FieldKey.OTP_COUNTER,
            EntryDraftValue.LongNumber(counter.trim().toLongOrNull() ?: 0L),
        )
    } else {
        draft.withValue(
            definition,
            FieldKey.OTP_PERIOD,
            EntryDraftValue.Number(period.trim().toIntOrNull() ?: 30),
        )
    }
    draft = draft.withOptionalText(definition, FieldKey.OTP_ISSUER, issuer)
    draft = draft.withOptionalText(definition, FieldKey.OTP_ACCOUNT_NAME, accountName)
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
    val values = rawTags.split(',').map(String::trim).filter(String::isNotEmpty)
    return if (values.isEmpty()) this else withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
}
