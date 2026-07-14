package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.WifiPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toWifiPayload(): WifiPayload? {
    if (wifiSecurityType == "WPA" && !wifiIsHidden) {
        return null
    }
    return WifiPayload(
        securityType = wifiSecurityType,
        isHidden = wifiIsHidden
    )
}

fun VaultEntry.mergeWifi(payload: WifiPayload?): VaultEntry {
    payload ?: return this
    return copy(
        wifiSecurityType = payload.securityType,
        wifiIsHidden = payload.isHidden
    )
}