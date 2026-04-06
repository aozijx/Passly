package com.aozijx.passly.domain.strategy

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.VaultEntry

/**
 * 条目类型策略基类
 */
interface EntryTypeStrategy {
    val entryType: EntryType

    /**
     * 根据字段名获取对应的显示标签（用于复制提示）
     */
    fun getCopyLabel(fieldKey: String): String {
        return when (fieldKey) {
            "password" -> "密码"
            "username" -> "账号"
            else -> "内容"
        }
    }

    /**
     * 根据 FieldKey 从条目中提取数据值
     */
    fun getFieldValue(entry: VaultEntry, key: FieldKey): String? {
        return when (key) {
            FieldKey.TITLE -> entry.title
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
            FieldKey.WIFI_SECURITY -> entry.wifiSecurityType
            FieldKey.WIFI_HIDDEN -> if (entry.wifiIsHidden) "是" else "否"
            FieldKey.CARD_EXPIRATION -> entry.cardExpiration
            FieldKey.CARD_CVV -> entry.cardCvv
            FieldKey.ID_NUMBER -> entry.idNumber
            FieldKey.PAYMENT_PIN -> entry.paymentPin
            FieldKey.PAYMENT_PLATFORM -> entry.paymentPlatform
            FieldKey.SECURITY_QUESTION -> entry.securityQuestion
            FieldKey.SECURITY_ANSWER -> entry.securityAnswer
            FieldKey.SSH_KEY -> entry.sshPrivateKey
            FieldKey.SEED_PHRASE -> entry.cryptoSeedPhrase
        }
    }

    /**
     * 获取详情页面的字段分组定义
     */
    fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup>

    fun validateRequiredFields(entry: VaultEntry): String?
    fun validateFieldContent(entry: VaultEntry): String?
    fun getSensitiveFields(): Set<String>
    fun extractSummary(entry: VaultEntry): String
    fun suggestedCategory(): String
    fun supportsAutofill(): Boolean
    fun initializeDefaults(entry: VaultEntry): VaultEntry = entry
    fun cleanup(entry: VaultEntry): VaultEntry = entry
}

object EntryTypeStrategyFactory {
    private val strategies = mutableMapOf<EntryType, EntryTypeStrategy>()

    fun register(strategy: EntryTypeStrategy) {
        strategies[strategy.entryType] = strategy
    }

    fun getStrategy(entryType: EntryType): EntryTypeStrategy {
        return strategies[entryType]
            ?: throw IllegalArgumentException("没有找到类型 $entryType 对应的策略")
    }

    fun getStrategy(typeValue: Int): EntryTypeStrategy {
        val entryType = EntryType.fromValue(typeValue)
        return getStrategy(entryType)
    }

    fun hasStrategy(entryType: EntryType): Boolean = entryType in strategies
    fun getAllStrategies(): List<EntryTypeStrategy> = strategies.values.toList()
}
