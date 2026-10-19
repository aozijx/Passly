package com.aozijx.passly.presentation.feature.vault.editor.wifi

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryViolationCode
import com.aozijx.passly.domain.entry.policy.WifiCredentialValidation
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload

data class AddWifiFormState(
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

sealed interface AddWifiInitialContent {
    data object Blank : AddWifiInitialContent
    data class Scanned(val payload: WifiQrPayload) : AddWifiInitialContent
    data object SeedUnavailable : AddWifiInitialContent
}

data class AddWifiUiState(
    val form: AddWifiFormState = AddWifiFormState(),
    val fieldErrors: Map<FieldKey, EntryViolationCode> = form.visibleFieldErrors(),
    val savePhase: AddWifiSavePhase = AddWifiSavePhase.Idle,
) {
    val canSave: Boolean
        get() = form.fieldErrors().isEmpty() && savePhase == AddWifiSavePhase.Idle
}

internal fun AddWifiFormState.fieldErrors(): Map<FieldKey, EntryViolationCode> = buildMap {
    WifiCredentialValidation.validate(
        ssid = ssid,
        password = password.takeIf(String::isNotEmpty),
        securityType = securityType,
    ).forEach { put(it.field, it.code) }
}

private fun AddWifiFormState.visibleFieldErrors(): Map<FieldKey, EntryViolationCode> =
    fieldErrors().filterValues { it != EntryViolationCode.REQUIRED }

internal fun AddWifiUiState.withForm(form: AddWifiFormState): AddWifiUiState {
    return copy(
        form = form,
        fieldErrors = form.visibleFieldErrors(),
    )
}
