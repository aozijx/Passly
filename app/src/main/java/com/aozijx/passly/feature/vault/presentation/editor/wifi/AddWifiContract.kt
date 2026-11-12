package com.aozijx.passly.feature.vault.presentation.editor.wifi

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryViolation
import com.aozijx.passly.domain.entry.policy.EntryViolationCode
import com.aozijx.passly.domain.entry.policy.WifiCredentialValidation
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

data class AddWifiFormState(
    val ssid: String = "",
    val securityType: WifiSecurityType = WifiSecurityType.WpaPersonal,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isHidden: Boolean = false,
    val notes: String = "",
    val tags: String = "",
) {
    val showsPasswordField: Boolean
        get() = securityType != WifiSecurityType.Open
}

internal fun WifiSecurityType.labelResource(): Int =
    when (this) {
        WifiSecurityType.Open -> R.string.wifi_security_open
        WifiSecurityType.Wep -> R.string.wifi_security_wep
        WifiSecurityType.WpaPersonal -> R.string.wifi_security_wpa_personal
    }

enum class WifiPasswordFeedbackUiState { Hidden, Invalid }

sealed interface AddWifiInitialContent {
    data object Blank : AddWifiInitialContent

    data class Scanned(
        val payload: WifiQrPayload,
    ) : AddWifiInitialContent

    data object SeedUnavailable : AddWifiInitialContent
}

enum class AddWifiInitializationResult { Ready, Unavailable, AlreadyInitialized }

enum class AddWifiInitializationGate { NeedsInitialContent, Unavailable, AlreadyInitialized }

data class AddWifiUiState(
    val form: AddWifiFormState = AddWifiFormState(),
    val isSaving: Boolean = false,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val passwordFeedback: WifiPasswordFeedbackUiState
        get() =
            if (
                form.showsPasswordField &&
                form.validationViolations().any { it.field == FieldKey.PASSWORD && it.code != EntryViolationCode.REQUIRED }
            ) {
                WifiPasswordFeedbackUiState.Invalid
            } else {
                WifiPasswordFeedbackUiState.Hidden
            }

    val canSave: Boolean
        get() =
            form.validationViolations().isEmpty() && !isSaving && loadPhase.allowsSaving
}

internal fun AddWifiFormState.validationViolations(): Set<EntryViolation> =
    WifiCredentialValidation.validate(
        ssid = ssid,
        password = password.takeIf(String::isNotEmpty),
        securityType = securityType,
    )
