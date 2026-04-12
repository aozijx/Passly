package com.aozijx.passly.domain.strategy

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.core.VaultEntry

/**
 * 条目类型策略基类
 */
interface EntryTypeStrategy {
    val entryType: EntryType

    /**
     * 根据 FieldKey 获取对应的显示标签（用于复制提示）
     */
    fun getCopyLabel(key: FieldKey): String = when (key) {
        FieldKey.PASSWORD -> "密码"
        FieldKey.USERNAME -> "账号"
        FieldKey.CARD_CVV -> "CVV"
        FieldKey.PAYMENT_PIN -> "支付密码"
        FieldKey.SSH_KEY -> "SSH 私钥"
        FieldKey.SEED_PHRASE -> "助记词"
        FieldKey.ID_NUMBER -> "证件号"
        FieldKey.PASSKEY_DATA -> "Passkey"
        FieldKey.RECOVERY_CODES -> "恢复码"
        FieldKey.NOTES -> "备注"
        else -> "内容"
    }

    /**
     * 根据 FieldKey 从条目中提取数据值
     * 采用分组拆分逻辑，提高可维护性
     */
    fun getFieldValue(entry: VaultEntry, key: FieldKey): String? {
        return when {
            key.isCommon() -> getCommonFieldValue(entry, key)
            key.isTotp() -> getTotpFieldValue(entry, key)
            key.isCrypto() -> getCryptoFieldValue(entry, key)
            key.isFinance() -> getFinanceFieldValue(entry, key)
            key.isIdentity() -> getIdentityFieldValue(entry, key)
            key.isConnectivity() -> getConnectivityFieldValue(entry, key)
            else -> null
        }
    }

    // --- 分组提取逻辑 (私有辅助函数) ---

    private fun getCommonFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.TITLE -> entry.title
        FieldKey.USERNAME -> entry.username
        FieldKey.PASSWORD -> entry.password
        FieldKey.NOTES -> entry.notes
        FieldKey.URIS -> entry.uriList?.joinToString(", ")
        else -> null
    }

    private fun getTotpFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.TOTP_SECRET -> entry.totpSecret
        FieldKey.TOTP_PERIOD -> entry.totpPeriod.toString()
        FieldKey.TOTP_DIGITS -> entry.totpDigits.toString()
        FieldKey.TOTP_ALGORITHM -> entry.totpAlgorithm
        else -> null
    }

    private fun getCryptoFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.PASSKEY_DATA -> entry.passkeyDataJson
        FieldKey.RECOVERY_CODES -> entry.recoveryCodes
        FieldKey.HARDWARE_INFO -> entry.hardwareKeyInfo
        FieldKey.SSH_KEY -> entry.sshPrivateKey
        FieldKey.SEED_PHRASE -> entry.cryptoSeedPhrase
        else -> null
    }

    private fun getFinanceFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.CARD_EXPIRATION -> entry.cardExpiration
        FieldKey.CARD_CVV -> entry.cardCvv
        FieldKey.PAYMENT_PIN -> entry.paymentPin
        FieldKey.PAYMENT_PLATFORM -> entry.paymentPlatform
        FieldKey.SECURITY_QUESTION -> entry.securityQuestion
        FieldKey.SECURITY_ANSWER -> entry.securityAnswer
        else -> null
    }

    private fun getIdentityFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.ID_NUMBER -> entry.idNumber
        else -> null
    }

    private fun getConnectivityFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.WIFI_SECURITY -> entry.wifiSecurityType
        FieldKey.WIFI_HIDDEN -> if (entry.wifiIsHidden) "是" else "否"
        else -> null
    }

    // --- FieldKey 扩展判定 (建议定义在 FieldKey 枚举中，此处仅作演示) ---
    private fun FieldKey.isCommon() = this in listOf(
        FieldKey.TITLE, FieldKey.USERNAME, FieldKey.PASSWORD, FieldKey.NOTES, FieldKey.URIS
    )

    private fun FieldKey.isTotp() = this.name.startsWith("TOTP")
    private fun FieldKey.isCrypto() = this in listOf(
        FieldKey.PASSKEY_DATA,
        FieldKey.RECOVERY_CODES,
        FieldKey.HARDWARE_INFO,
        FieldKey.SSH_KEY,
        FieldKey.SEED_PHRASE
    )

    private fun FieldKey.isFinance() =
        this.name.startsWith("CARD") || this.name.startsWith("PAYMENT") || this.name.contains("SECURITY")

    private fun FieldKey.isIdentity() = this == FieldKey.ID_NUMBER
    private fun FieldKey.isConnectivity() = this.name.startsWith("WIFI")


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
}