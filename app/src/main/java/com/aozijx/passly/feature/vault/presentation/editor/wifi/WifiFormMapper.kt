package com.aozijx.passly.feature.vault.presentation.editor.wifi

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.presentation.editor.parseRawTags

fun AddWifiFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.WIFI]
    var draft = EntryDraft(EntryType.WIFI)
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(ssid))
        .withValue(definition, FieldKey.WIFI_SSID, EntryDraftValue.Text(ssid))
        .withValue(
            definition,
            FieldKey.WIFI_SECURITY,
            EntryDraftValue.Text(securityType.storedValue),
        )
        .withValue(definition, FieldKey.WIFI_HIDDEN, EntryDraftValue.Toggle(isHidden))

    if (password.isNotEmpty()) {
        draft = draft.withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(password))
    }
    draft = draft.withOptionalText(definition, FieldKey.NOTES, notes)
    return draft.withTags(definition, tags)
}

internal fun AddWifiFormState.withSsid(value: String): AddWifiFormState = copy(ssid = value)

internal fun AddWifiFormState.withSecurityType(value: WifiSecurityType): AddWifiFormState = copy(
    securityType = value,
    password = password.takeUnless { value == WifiSecurityType.Open }.orEmpty(),
    isPasswordVisible = isPasswordVisible && value != WifiSecurityType.Open,
)

internal fun AddWifiFormState.applyScanned(payload: WifiQrPayload): AddWifiFormState {
    return copy(
        ssid = payload.ssid,
        securityType = payload.securityType,
        password = payload.password.takeUnless {
            payload.securityType == WifiSecurityType.Open
        }.orEmpty(),
        isPasswordVisible = false,
        isHidden = payload.isHidden,
    )
}

private fun EntryDraft.withOptionalText(
    definition: EntryTypeDefinition,
    key: FieldKey,
    rawValue: String,
): EntryDraft = rawValue.trim().takeIf(String::isNotEmpty)
    ?.let { withValue(definition, key, EntryDraftValue.Text(it)) }
    ?: this

private fun EntryDraft.withTags(
    definition: EntryTypeDefinition,
    rawTags: String,
): EntryDraft {
    val values = parseRawTags(rawTags).distinct()
    return if (values.isEmpty()) this else {
        withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
    }
}
