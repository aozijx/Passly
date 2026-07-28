package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

/**
 * OTP 字段状态。保存中、错误等页面生命周期状态由对应编辑器 ViewModel 持有。
 */
data class OtpFormState(
    val title: String = "",
    val username: String = "",
    val domain: String = "",
    val issuer: String = "",
    val secret: String = "",
    val period: String = "30",
    val digits: String = "6",
    val type: OtpType = OtpType.TOTP,
    val algorithm: String = "SHA1",
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val counter: String = "0",
    val uriText: String = ""
) {
    val isValid: Boolean get() = title.isNotBlank() && secret.isNotBlank()
}
