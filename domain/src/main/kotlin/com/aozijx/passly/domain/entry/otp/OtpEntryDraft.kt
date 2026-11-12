package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions

/** A creation draft from an existing configuration; identity metadata is not form-normalized. */
fun OtpConfig.toNewEntryDraft(title: String = suggestedEntryTitle()): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.OTP]

    var draft =
        EntryDraft(EntryType.OTP)
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title))
            .withValue(
                definition,
                FieldKey.OTP_SECRET,
                EntryDraftValue.Text(requireNotNull(secret) { "OTP secret is unavailable" }),
            ).withValue(definition, FieldKey.OTP_TYPE, EntryDraftValue.Text(OtpTypeDefinitions[type].key))
            .withValue(definition, FieldKey.OTP_ENCODING, EntryDraftValue.Text(encoding.name))

    draft =
        when (this) {
            is OtpConfig.Totp -> {
                draft
                    .withValue(definition, FieldKey.OTP_DIGITS, EntryDraftValue.Number(digits))
                    .withValue(definition, FieldKey.OTP_ALGORITHM, EntryDraftValue.Text(algorithm.name))
                    .withValue(definition, FieldKey.OTP_PERIOD, EntryDraftValue.Number(periodSeconds))
            }

            is OtpConfig.Hotp -> {
                draft
                    .withValue(definition, FieldKey.OTP_DIGITS, EntryDraftValue.Number(digits))
                    .withValue(definition, FieldKey.OTP_ALGORITHM, EntryDraftValue.Text(algorithm.name))
                    .withValue(definition, FieldKey.OTP_COUNTER, EntryDraftValue.LongNumber(counter))
            }
        }
    issuer?.takeIf(String::isNotBlank)?.let {
        draft = draft.withValue(definition, FieldKey.OTP_ISSUER, EntryDraftValue.Text(it))
    }
    accountName?.takeIf(String::isNotBlank)?.let {
        draft = draft.withValue(definition, FieldKey.OTP_ACCOUNT_NAME, EntryDraftValue.Text(it))
    }
    return draft
}

/** Display-only trimming must not change the stored issuer or account. */
fun OtpConfig.suggestedEntryTitle(fallback: String = OtpTypeDefinitions[type].defaultEntryTitle): String =
    listOfNotNull(
        issuer?.trim()?.takeIf(String::isNotEmpty),
        accountName?.trim()?.takeIf(String::isNotEmpty),
    ).joinToString(": ").ifEmpty { fallback }
