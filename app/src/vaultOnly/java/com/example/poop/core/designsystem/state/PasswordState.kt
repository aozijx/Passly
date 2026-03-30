package com.example.poop.core.designsystem.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 密码新增表单状态
 */
class PasswordAddState {
    var title by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var category by mutableStateOf("")
    var isPasswordVisible by mutableStateOf(false)

    val isValid: Boolean get() = title.isNotBlank() && password.isNotBlank()
}
