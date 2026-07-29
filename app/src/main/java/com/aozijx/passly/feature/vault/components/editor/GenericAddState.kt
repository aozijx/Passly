package com.aozijx.passly.feature.vault.components.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 通用条目新增表单状态（银行卡、WiFi、SSH密钥、证件等）
 */
class GenericAddState {
    var title by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var notes by mutableStateOf("")
    var isPasswordVisible by mutableStateOf(false)
}
