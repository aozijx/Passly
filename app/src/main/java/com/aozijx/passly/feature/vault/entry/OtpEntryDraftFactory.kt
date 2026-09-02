package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions

internal fun OtpConfig.toNewEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.OTP]
    val title = listOfNotNull(
        issuer?.trim()?.takeIf(String::isNotEmpty),
        accountName?.trim()?.takeIf(String::isNotEmpty),
    ).joinToString(": ").ifEmpty { "TOTP" }

    var draft = EntryDraft(EntryDraftTarget.New(EntryType.OTP))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title))
        .withValue(
            definition,
            FieldKey.OTP_SECRET,
            EntryDraftValue.Text(requireNotNull(secret) { "Scanned OTP secret is unavailable" }),
        )
        .withValue(definition, FieldKey.OTP_TYPE, EntryDraftValue.Text(type.name))
        .withValue(definition, FieldKey.OTP_DIGITS, EntryDraftValue.Number(digits))
        .withValue(definition, FieldKey.OTP_ALGORITHM, EntryDraftValue.Text(algorithm.name))
        .withValue(definition, FieldKey.OTP_ENCODING, EntryDraftValue.Text(encoding.name))

    draft = if (type == OtpType.HOTP) {
        draft.withValue(definition, FieldKey.OTP_COUNTER, EntryDraftValue.LongNumber(counter))
    } else {
        draft.withValue(definition, FieldKey.OTP_PERIOD, EntryDraftValue.Number(periodSeconds))
    }
    issuer?.trim()?.takeIf(String::isNotEmpty)?.let {
        draft = draft.withValue(definition, FieldKey.OTP_ISSUER, EntryDraftValue.Text(it))
    }
    accountName?.trim()?.takeIf(String::isNotEmpty)?.let {
        draft = draft.withValue(definition, FieldKey.OTP_ACCOUNT_NAME, EntryDraftValue.Text(it))
    }
    return draft
}
