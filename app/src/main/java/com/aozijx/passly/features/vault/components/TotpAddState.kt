package com.aozijx.passly.features.vault.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * TOTP 新增表单状态
 */
class TotpAddState {
    var title by mutableStateOf("")
    var username by mutableStateOf("")
    var category by mutableStateOf("")
    var domain by mutableStateOf("")

    // TOTP 核心配置
    var secret by mutableStateOf("")
    var period by mutableStateOf("30")
    var digits by mutableStateOf("6")
    var algorithm by mutableStateOf("SHA1")

    // UI 控制状态
    var uriText by mutableStateOf("")
    var showAdvanced by mutableStateOf(false)

    val isValid: Boolean get() = title.isNotBlank() && secret.isNotBlank()
}
