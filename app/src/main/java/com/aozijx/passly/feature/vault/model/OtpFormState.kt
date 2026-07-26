package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

/**
 * 统一的 OTP 表单状态，适用于添加和编辑两种模式。
 */
data class OtpFormState(
    val mode: Mode = Mode.Add,
    val title: String = "",
    val username: String = "",
    val domain: String = "",
    val issuer: String = "",
    val category: String = "",
    val secret: String = "",
    val period: String = "30",
    val digits: String = "6",
    val type: OtpType = OtpType.TOTP,
    val algorithm: String = "SHA1",
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val counter: String = "0",
    val uriText: String = "",
    val showAdvanced: Boolean = true
) {
    val isValid: Boolean get() = title.isNotBlank() && secret.isNotBlank()

    sealed interface Mode {
        data object Add : Mode
        data class Edit(val entryId: String, val entryVersion: Int) : Mode
    }

    companion object {
        fun fromEntry(entry: VaultEntry): OtpFormState {
            val config = entry.secret.otp?.config
            return OtpFormState(
                mode = Mode.Edit(entry.id, entry.entryVersion),
                title = entry.title,
                username = entry.summary.username,
                domain = entry.associatedDomain ?: "",
                issuer = config?.issuer ?: "",
                category = entry.category,
                secret = config?.secret ?: "",
                period = (config?.periodSeconds ?: 30).toString(),
                digits = (config?.digits ?: 6).toString(),
                type = config?.type ?: OtpType.TOTP,
                algorithm = config?.algorithm?.name ?: "SHA1",
                encoding = config?.encoding ?: OtpSecretEncoding.BASE32,
                counter = (config?.counter ?: 0L).toString()
            )
        }
    }
}
