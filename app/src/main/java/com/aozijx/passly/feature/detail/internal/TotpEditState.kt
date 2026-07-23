package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType

/**
 * TOTP 编辑状态（详情页修改配置）
 */
class TotpEditState(entry: VaultEntry, initialSecret: String) {
    val otpConfig = (entry.secret as? EntrySecret.Otp)?.data?.config
    var isEditing by mutableStateOf(false)
    var secret by mutableStateOf(initialSecret)
    var period by mutableStateOf((otpConfig?.periodSeconds ?: 30).toString())
    var digits by mutableStateOf((otpConfig?.digits ?: 6).toString())
    var type by mutableStateOf(otpConfig?.type ?: OtpType.TOTP)
    var algorithm by mutableStateOf(otpConfig?.algorithm?.name ?: "SHA1")
    var encoding by mutableStateOf(
        otpConfig?.encoding ?: OtpSecretEncoding.BASE32
    )
    var counter by mutableStateOf((otpConfig?.counter ?: 0L).toString())

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
