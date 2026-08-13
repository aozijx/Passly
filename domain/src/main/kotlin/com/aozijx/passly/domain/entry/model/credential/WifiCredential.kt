package com.aozijx.passly.domain.entry.model.credential


data class WifiCredential(
    val ssid: String,
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false
) : EntryCredential {
    init {
        require(ssid.isNotBlank()) { "Wi-Fi SSID cannot be blank" }
    }

    override val kind: EntryCredentialKind = EntryCredentialKind.WIFI
}
