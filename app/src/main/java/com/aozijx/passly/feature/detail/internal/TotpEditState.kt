package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * TOTP 编辑状态（详情页修改配置）
 */
class TotpEditState(entry: VaultEntry, initialSecret: String) {
    var isEditing by mutableStateOf(false)
    var secret by mutableStateOf(initialSecret)
    var period by mutableStateOf((entry.credential.twoFactor?.otp?.period ?: 30).toString())
    var digits by mutableStateOf((entry.credential.twoFactor?.otp?.digits ?: 6).toString())
    var algorithm by mutableStateOf(entry.credential.twoFactor?.otp?.algorithm ?: "SHA1")

    fun applySteamPreset() {
        algorithm = "STEAM"
        digits = "5"
    }
}