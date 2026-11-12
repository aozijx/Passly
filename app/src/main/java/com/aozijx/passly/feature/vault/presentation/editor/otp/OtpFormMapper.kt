package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
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
        period = when (config) {
            is OtpConfig.Totp -> config.periodSeconds.toString()
            is OtpConfig.Steam -> SteamDefinition.PERIOD_SECONDS.toString()
            is OtpConfig.Hotp, null -> TotpDefinition.DEFAULT_PERIOD_SECONDS.toString()
        },
        digits = config?.digits?.toString() ?: TotpDefinition.DEFAULT_DIGITS.toString(),
        type = config?.type ?: OtpType.TOTP,
        algorithm = config?.algorithm ?: OtpHashAlgorithm.SHA1,
        encoding = config?.encoding ?: OtpSecretEncoding.BASE32,
        counter = (config as? OtpConfig.Hotp)?.counter?.toString() ?: HotpDefinition.INITIAL_COUNTER.toString(),
        uriText = "",
        notes = entry.secret.notes.orEmpty(),
        tags = entry.tags.joinToString(", "),
    )
}
