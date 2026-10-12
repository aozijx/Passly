package com.aozijx.passly.presentation.feature.vault.editor.wifi

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload

sealed interface AddWifiAction {
    data class SsidChanged(val value: String) : AddWifiAction
    data class SecurityTypeChanged(val value: WifiSecurityType) : AddWifiAction
    data class PasswordChanged(val value: String) : AddWifiAction
    data class PasswordVisibilityChanged(val visible: Boolean) : AddWifiAction
    data class HiddenChanged(val hidden: Boolean) : AddWifiAction
    data class NotesChanged(val value: String) : AddWifiAction
    data class TagsChanged(val value: String) : AddWifiAction
    data class ApplyScannedWifi(val payload: WifiQrPayload) : AddWifiAction
    data object Save : AddWifiAction
}
