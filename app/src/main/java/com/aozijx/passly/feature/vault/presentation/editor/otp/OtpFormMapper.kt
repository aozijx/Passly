package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.otp.OtpConfigInputResult
import com.aozijx.passly.domain.entry.otp.toNewEntryDraft
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.feature.vault.presentation.editor.withOptionalText
import com.aozijx.passly.feature.vault.presentation.editor.withTags

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
        configuration =
            when (config) {
                is OtpConfig.Totp -> {
                    OtpFormConfiguration.Totp(
                        algorithm = config.algorithm,
                        digits = config.digits.toString(),
                        periodSeconds = config.periodSeconds.toString(),
                        encoding = config.encoding,
                    )
                }

                is OtpConfig.Hotp -> {
                    OtpFormConfiguration.Hotp(
                        algorithm = config.algorithm,
                        digits = config.digits.toString(),
                        counter = config.counter.toString(),
                        encoding = config.encoding,
                    )
                }

                is OtpConfig.Steam -> {
                    OtpFormConfiguration.Steam(config.encoding)
                }

                null -> {
                    OtpFormConfiguration.Totp(encoding = OtpSecretEncoding.BASE32)
                }
            },
        uriText = "",
        notes = entry.secret.notes.orEmpty(),
        tags = entry.tags.joinToString(", "),
    )
}
