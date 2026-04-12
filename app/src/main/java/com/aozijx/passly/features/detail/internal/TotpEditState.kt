package com.aozijx.passly.features.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.VaultEntry

/**
 * TOTP 编辑状态（详情页修改配置）
 */
class TotpEditState(entry: VaultEntry, initialSecret: String) {
    var isEditing by mutableStateOf(false)
    var secret by mutableStateOf(initialSecret)
    var period by mutableStateOf(entry.totpPeriod.toString())
    var digits by mutableStateOf(entry.totpDigits.toString())
    var algorithm by mutableStateOf(entry.totpAlgorithm)

    fun applySteamPreset() {
        algorithm = "STEAM"
        digits = "5"
    }
}
