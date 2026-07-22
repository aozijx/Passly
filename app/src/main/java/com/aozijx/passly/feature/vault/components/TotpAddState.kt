package com.aozijx.passly.feature.vault.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.OtpSecretEncoding
import com.aozijx.passly.domain.model.core.OtpType

class TotpAddState {
    var title by mutableStateOf("")
    var username by mutableStateOf("")
    var category by mutableStateOf("")
    var domain by mutableStateOf("")
    var issuer by mutableStateOf("")

    // TOTP 核心配置
    var secret by mutableStateOf("")
    var period by mutableStateOf("30")
    var digits by mutableStateOf("6")
    var type by mutableStateOf(OtpType.TOTP)
    var algorithm by mutableStateOf("SHA1")
    var encoding by mutableStateOf(OtpSecretEncoding.BASE32)
    var counter by mutableStateOf("0")

    // UI 控制状态
    var uriText by mutableStateOf("")

    // OTP 类型不能隐藏在默认折叠区，否则手工输入 Steam Secret 会被保存为普通 TOTP。
    var showAdvanced by mutableStateOf(true)

    val isValid: Boolean get() = title.isNotBlank() && secret.isNotBlank()

    fun selectType(value: OtpType) {
        type = value
        when (value) {
            OtpType.STEAM -> {
                digits = "5"
                period = "30"
                algorithm = "SHA1"
            }

            OtpType.TOTP -> {
                if (digits == "5") digits = "6"
                if (period.isBlank()) period = "30"
            }

            OtpType.HOTP -> if (counter.isBlank()) counter = "0"
        }
    }
}
