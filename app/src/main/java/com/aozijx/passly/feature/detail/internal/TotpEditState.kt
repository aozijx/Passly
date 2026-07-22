package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.OtpSecretEncoding
import com.aozijx.passly.domain.model.core.OtpType
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * TOTP 编辑状态（详情页修改配置）
 */
class TotpEditState(entry: VaultEntry, initialSecret: String) {
    var isEditing by mutableStateOf(false)
    var secret by mutableStateOf(initialSecret)
    var period by mutableStateOf((entry.credential.otp?.periodSeconds ?: 30).toString())
    var digits by mutableStateOf((entry.credential.otp?.digits ?: 6).toString())
    var type by mutableStateOf(entry.credential.otp?.type ?: OtpType.TOTP)
    var algorithm by mutableStateOf(entry.credential.otp?.algorithm?.name ?: "SHA1")
    var encoding by mutableStateOf(
        entry.credential.otp?.encoding ?: OtpSecretEncoding.BASE32
    )
    var counter by mutableStateOf((entry.credential.otp?.counter ?: 0L).toString())

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
