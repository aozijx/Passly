package com.aozijx.passly.core.designsystem.utils

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry

/**
 * 根据 entryType 动态确定应该显示哪些字段及其标签
 */
object EntryTypeResolver {
    
    /**
     * 获取条目类型对应的字段显示配置
     */
    fun getFieldLabels(vaultType: EntryType): List<FieldLabel> {
        return when (vaultType) {
            EntryType.PASSWORD -> listOf(
                FieldLabel("用户名", FieldKey.USERNAME),
                FieldLabel("密码", FieldKey.PASSWORD),
                FieldLabel("网址", FieldKey.URIS),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.TOTP -> listOf(
                FieldLabel("密钥", FieldKey.TOTP_SECRET),
                FieldLabel("周期", FieldKey.TOTP_PERIOD),
                FieldLabel("位数", FieldKey.TOTP_DIGITS),
                FieldLabel("算法", FieldKey.TOTP_ALGORITHM),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.PASSKEY -> listOf(
                FieldLabel("Passkey数据", FieldKey.PASSKEY_DATA),
                FieldLabel("恢复码", FieldKey.RECOVERY_CODES),
                FieldLabel("硬件密钥", FieldKey.HARDWARE_INFO),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.WIFI -> listOf(
                FieldLabel("SSID", FieldKey.USERNAME),
                FieldLabel("密码", FieldKey.PASSWORD),
                FieldLabel("加密类型", FieldKey.WIFI_ENCRYPTION),
                FieldLabel("隐藏SSID", FieldKey.WIFI_HIDDEN),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.BANK_CARD -> listOf(
                FieldLabel("持卡人", FieldKey.USERNAME),
                FieldLabel("卡号", FieldKey.PASSWORD),
                FieldLabel("有效期", FieldKey.CARD_EXPIRATION),
                FieldLabel("CVV", FieldKey.CARD_CVV),
                FieldLabel("支付密码", FieldKey.PAYMENT_PIN),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.SEED_PHRASE -> listOf(
                FieldLabel("助记词", FieldKey.SEED_PHRASE),
                FieldLabel("密码", FieldKey.PASSWORD),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.ID_CARD -> listOf(
                FieldLabel("证件号", FieldKey.ID_NUMBER),
                FieldLabel("持有人", FieldKey.USERNAME),
                FieldLabel("有效期", FieldKey.CARD_EXPIRATION),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.SSH_KEY -> listOf(
                FieldLabel("私钥", FieldKey.SSH_KEY),
                FieldLabel("用户名", FieldKey.USERNAME),
                FieldLabel("主机", FieldKey.URIS),
                FieldLabel("备注", FieldKey.NOTES)
            )
            
            EntryType.RECOVERY_CODE -> listOf(
                FieldLabel("恢复码", FieldKey.RECOVERY_CODES),
                FieldLabel("所属账户", FieldKey.USERNAME),
                FieldLabel("备注", FieldKey.NOTES)
            )
        }
    }
    
    /**
     * 从条目中提取特定字段的值
     */
    fun extractField(entry: VaultEntry, key: FieldKey): String? {
        return when (key) {
            FieldKey.USERNAME -> entry.username
            FieldKey.PASSWORD -> entry.password
            FieldKey.NOTES -> entry.notes
            FieldKey.URIS -> entry.uriList?.joinToString(", ")
            FieldKey.TOTP_SECRET -> entry.totpSecret
            FieldKey.TOTP_PERIOD -> entry.totpPeriod.toString()
            FieldKey.TOTP_DIGITS -> entry.totpDigits.toString()
            FieldKey.TOTP_ALGORITHM -> entry.totpAlgorithm
            FieldKey.PASSKEY_DATA -> entry.passkeyDataJson
            FieldKey.RECOVERY_CODES -> entry.recoveryCodes
            FieldKey.HARDWARE_INFO -> entry.hardwareKeyInfo
            FieldKey.WIFI_ENCRYPTION -> entry.wifiEncryptionType
            FieldKey.WIFI_HIDDEN -> if (entry.wifiIsHidden) "是" else "否"
            FieldKey.CARD_EXPIRATION -> entry.cardExpiration
            FieldKey.CARD_CVV -> entry.cardCvv
            FieldKey.PAYMENT_PIN -> entry.paymentPin
            FieldKey.SEED_PHRASE -> entry.cryptoSeedPhrase
            FieldKey.ID_NUMBER -> entry.idNumber
            FieldKey.SSH_KEY -> entry.sshPrivateKey
        }
    }
    
    /**
     * 获取主要显示字段（用于列表摘要）
     */
    fun getPrimaryField(vaultType: EntryType): FieldKey {
        return when (vaultType) {
            EntryType.PASSWORD, EntryType.PASSKEY, EntryType.ID_CARD, EntryType.RECOVERY_CODE -> FieldKey.USERNAME
            EntryType.WIFI -> FieldKey.USERNAME // SSID
            EntryType.BANK_CARD -> FieldKey.PASSWORD // 卡号
            EntryType.SSH_KEY -> FieldKey.URIS // 主机
            EntryType.SEED_PHRASE -> FieldKey.SEED_PHRASE
            EntryType.TOTP -> FieldKey.TOTP_SECRET
        }
    }
    
    /**
     * 获取次要显示字段（用于列表摘要）
     */
    fun getSecondaryField(vaultType: EntryType): FieldKey? {
        return when (vaultType) {
            EntryType.PASSWORD, EntryType.SSH_KEY -> FieldKey.URIS
            EntryType.BANK_CARD -> FieldKey.CARD_EXPIRATION
            EntryType.ID_CARD -> FieldKey.CARD_EXPIRATION
            EntryType.WIFI -> FieldKey.WIFI_ENCRYPTION
            else -> null
        }
    }
}

data class FieldLabel(
    val label: String,
    val key: FieldKey
)

enum class FieldKey {
    USERNAME,
    PASSWORD,
    NOTES,
    URIS,
    TOTP_SECRET,
    TOTP_PERIOD,
    TOTP_DIGITS,
    TOTP_ALGORITHM,
    PASSKEY_DATA,
    RECOVERY_CODES,
    HARDWARE_INFO,
    WIFI_ENCRYPTION,
    WIFI_HIDDEN,
    CARD_EXPIRATION,
    CARD_CVV,
    PAYMENT_PIN,
    SEED_PHRASE,
    ID_NUMBER,
    SSH_KEY
}
