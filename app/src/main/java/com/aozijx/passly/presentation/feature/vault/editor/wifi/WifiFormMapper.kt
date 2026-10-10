package com.aozijx.passly.presentation.feature.vault.editor.wifi

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload

fun AddWifiFormState.toEntryDraft(): EntryDraft {
    val definition = EntryTypeDefinitions[EntryType.WIFI]
    var draft = EntryDraft(EntryDraftTarget.New(EntryType.WIFI))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title.trim()))
        .withValue(definition, FieldKey.WIFI_SSID, EntryDraftValue.Text(ssid.trim()))
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

internal fun AddWifiFormState.withTitle(value: String): AddWifiFormState = copy(
    title = value,
    titleOrigin = if (value.isBlank()) WifiTitleOrigin.EMPTY else WifiTitleOrigin.USER_EDITED,
)

internal fun AddWifiFormState.withSsid(value: String): AddWifiFormState {
    val followsSsid = titleOrigin != WifiTitleOrigin.USER_EDITED
    return copy(
        ssid = value,
        title = if (followsSsid) value.trim() else title,
        titleOrigin = when {
            !followsSsid -> WifiTitleOrigin.USER_EDITED
            value.isBlank() -> WifiTitleOrigin.EMPTY
            else -> WifiTitleOrigin.SSID_GENERATED
        },
    )
}

internal fun AddWifiFormState.withSecurityType(value: WifiSecurityType): AddWifiFormState = copy(
    securityType = value,
    password = password.takeUnless { value == WifiSecurityType.Open }.orEmpty(),
    isPasswordVisible = isPasswordVisible && value != WifiSecurityType.Open,
)

internal fun AddWifiFormState.applyScanned(payload: WifiQrPayload): AddWifiFormState {
    val followsSsid = titleOrigin != WifiTitleOrigin.USER_EDITED
    return copy(
        title = if (followsSsid) payload.ssid.trim() else title,
        titleOrigin = if (followsSsid) WifiTitleOrigin.SSID_GENERATED else titleOrigin,
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
    val values = rawTags.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    return if (values.isEmpty()) this else {
        withValue(definition, FieldKey.TAGS, EntryDraftValue.TextList(values))
    }
}
