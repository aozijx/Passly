package com.aozijx.passly.presentation.feature.vault.editor.wifi

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryViolationCode
import com.aozijx.passly.domain.entry.policy.WifiCredentialValidation

enum class WifiTitleOrigin {
    EMPTY,
    SSID_GENERATED,
    USER_EDITED,
}

data class AddWifiFormState(
    val title: String = "",
    val titleOrigin: WifiTitleOrigin = WifiTitleOrigin.EMPTY,
    val ssid: String = "",
    val securityType: WifiSecurityType = WifiSecurityType.WpaPersonal,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isHidden: Boolean = false,
    val notes: String = "",
    val tags: String = "",
)

sealed interface AddWifiSavePhase {
    data object Idle : AddWifiSavePhase
    data object Saving : AddWifiSavePhase
}

data class AddWifiUiState(
    val form: AddWifiFormState = AddWifiFormState(),
    val fieldErrors: Map<FieldKey, EntryViolationCode> = form.fieldErrors(),
    val savePhase: AddWifiSavePhase = AddWifiSavePhase.Idle,
) {
    val canSave: Boolean
        get() = fieldErrors.isEmpty() && savePhase == AddWifiSavePhase.Idle
}

internal fun AddWifiFormState.fieldErrors(): Map<FieldKey, EntryViolationCode> = buildMap {
    if (title.isBlank()) put(FieldKey.TITLE, EntryViolationCode.REQUIRED)
    WifiCredentialValidation.validate(
        ssid = ssid,
        password = password.takeIf(String::isNotEmpty),
        securityType = securityType,
    ).forEach { put(it.field, it.code) }
}

internal fun AddWifiUiState.withForm(form: AddWifiFormState): AddWifiUiState {
    return copy(
        form = form,
        fieldErrors = form.fieldErrors(),
    )
}
