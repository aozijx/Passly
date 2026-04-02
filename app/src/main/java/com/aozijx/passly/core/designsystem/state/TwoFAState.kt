package com.aozijx.passly.core.designsystem.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.data.model.VaultEntry

/**
 * TOTP 实时显示状态
 */
data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)

/**
 * TOTP 新增表单状态
 */
class TotpAddState {
    var title by mutableStateOf("")
    var username by mutableStateOf("")
    var category by mutableStateOf("")

    // TOTP 核心配置
    var secret by mutableStateOf("")
    var period by mutableStateOf("30")
    var digits by mutableStateOf("6")
    var algorithm by mutableStateOf("SHA1")

    // UI 控制状态
    var uriText by mutableStateOf("")
    var showAdvanced by mutableStateOf(false)

    val isValid: Boolean
        get() = title.isNotBlank() && secret.isNotBlank()
}

/**
 * TOTP 编辑状态（用于详情页修改配置）
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
