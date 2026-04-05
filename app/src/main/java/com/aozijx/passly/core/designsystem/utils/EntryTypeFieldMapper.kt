package com.aozijx.passly.core.designsystem.utils

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry

/**
 * 根据 entryType 确定字段的实际含义和用途
 * 用于规范化不同类型条目的字段访问
 */
object EntryTypeFieldMapper {

    /**
     * 获取条目的"用户名"字段实际含义
     */
    fun getUsernameFieldLabel(vaultType: EntryType): String {
        return when (vaultType) {
            EntryType.PASSWORD -> "用户名"
            EntryType.WIFI -> "SSID"
            EntryType.BANK_CARD -> "持卡人"
            EntryType.ID_CARD -> "持有人"
            EntryType.SSH_KEY -> "用户名"
            EntryType.RECOVERY_CODE -> "所属账户"
            EntryType.TOTP -> "账户"
            else -> "用户名"
        }
    }

    /**
     * 获取条目的"密码"字段实际含义
     */
    fun getPasswordFieldLabel(vaultType: EntryType): String {
        return when (vaultType) {
            EntryType.PASSWORD -> "密码"
            EntryType.WIFI -> "Wi-Fi密码"
            EntryType.BANK_CARD -> "卡号"
            EntryType.SEED_PHRASE -> "助记词"
            else -> "密码"
        }
    }

    /**
     * 判断是否应该在 Autofill 中使用该条目
     */
    fun isAutofillCompatible(vaultType: EntryType): Boolean {
        return vaultType in setOf(
            EntryType.PASSWORD,
            EntryType.SSH_KEY,
            EntryType.WIFI
        )
    }

    /**
     * 获取条目的敏感字段列表（防止被截屏/日志记录）
     */
    fun getSensitiveFields(vaultType: EntryType): Set<String> {
        return when (vaultType) {
            EntryType.PASSWORD -> setOf("password", "totpSecret")
            EntryType.WIFI -> setOf("password")
            EntryType.BANK_CARD -> setOf("password", "cardCvv", "paymentPin", "securityAnswer")
            EntryType.ID_CARD -> setOf("idNumber")
            EntryType.SSH_KEY -> setOf("sshPrivateKey")
            EntryType.SEED_PHRASE -> setOf("cryptoSeedPhrase")
            EntryType.PASSKEY -> setOf("passkeyDataJson", "recoveryCodes")
            EntryType.RECOVERY_CODE -> setOf("recoveryCodes")
            EntryType.TOTP -> setOf("totpSecret")
        }
    }

    /**
     * 获取条目应该显示的"摘要"信息（用于列表显示）
     */
    fun getSummaryInfo(entry: VaultEntry, vaultType: EntryType): String {
        return when (vaultType) {
            EntryType.PASSWORD -> entry.uriList?.firstOrNull() ?: ""
            EntryType.WIFI -> entry.wifiEncryptionType ?: "无"
            EntryType.BANK_CARD -> entry.cardExpiration ?: ""
            EntryType.ID_CARD -> entry.cardExpiration ?: ""
            EntryType.SSH_KEY -> entry.uriList?.firstOrNull() ?: ""
            EntryType.RECOVERY_CODE -> "已保存"
            EntryType.TOTP -> "OTP"
            EntryType.PASSKEY -> "Passkey"
            EntryType.SEED_PHRASE -> "已保存"
        }
    }

    /**
     * 获取该类型是否需要特殊的加密处理
     */
    fun requiresStrongEncryption(vaultType: EntryType): Boolean {
        return vaultType in setOf(
            EntryType.SSH_KEY,
            EntryType.SEED_PHRASE,
            EntryType.PASSKEY
        )
    }
}