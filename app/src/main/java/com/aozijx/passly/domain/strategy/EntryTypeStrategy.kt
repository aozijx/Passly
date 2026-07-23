package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 条目类型策略基类
 *
 * Domain 层职责：验证、字段提取、字段标识、摘要。
 * UI 层相关（标签、组件类型）由 [com.aozijx.passly.feature.vault.strategy.EntryTypeDisplayProvider] 提供。
 */
interface EntryTypeStrategy {
    val entryType: EntryType

    /**
     * 根据 FieldKey 从条目中提取数据值
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
        FieldKey.PASSWORD -> entry.credential.password
        FieldKey.EMAIL -> entry.credential.email
        FieldKey.NOTES -> entry.credential.notes
        FieldKey.URIS -> entry.website?.matchDomains?.joinToString(", ")
        else -> null
    }

    private fun getTotpFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.TOTP_SECRET -> entry.credential.otp?.secret
        FieldKey.TOTP_ISSUER -> entry.credential.otp?.issuer
        FieldKey.TOTP_PERIOD -> (entry.credential.otp?.periodSeconds ?: 30).toString()
        FieldKey.TOTP_DIGITS -> (entry.credential.otp?.digits ?: 6).toString()
        FieldKey.TOTP_ALGORITHM -> entry.credential.otp?.algorithm?.name ?: "SHA1"
        else -> null
    }

    private fun getCryptoFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.PASSKEY_DATA -> entry.credential.passkeyPrivateKeyReference
        FieldKey.RECOVERY_CODES -> entry.credential.recoveryCodes.joinToString("\n")
        FieldKey.HARDWARE_INFO -> entry.credential.hardwareKeyInfo
        FieldKey.SSH_KEY -> entry.credential.sshPrivateKey
        FieldKey.SEED_PHRASE -> entry.credential.seedPhrase
        else -> null
    }

    private fun getFinanceFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.CARD_EXPIRATION -> entry.credential.cardExpiry
        FieldKey.CARD_CVV -> entry.credential.cardCvv
        FieldKey.PAYMENT_PIN -> entry.credential.paymentPin
        FieldKey.PAYMENT_PLATFORM -> entry.credential.paymentPlatform
        FieldKey.SECURITY_QUESTION -> entry.credential.securityQuestion
        FieldKey.SECURITY_ANSWER -> entry.credential.securityAnswer
        else -> null
    }

    private fun getIdentityFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.ID_NUMBER -> entry.credential.idNumber
        else -> null
    }

    private fun getConnectivityFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.WIFI_SECURITY -> entry.credential.wifiSecurityType
        FieldKey.WIFI_HIDDEN -> if (entry.credential.wifiIsHidden) "是" else "否"
        else -> null
    }

    // --- FieldKey 扩展判定 (建议定义在 FieldKey 枚举中，此处仅作演示) ---
    private fun FieldKey.isCommon() = this in listOf(
        FieldKey.TITLE, FieldKey.USERNAME, FieldKey.PASSWORD, FieldKey.EMAIL, FieldKey.NOTES, FieldKey.URIS
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


    fun validateRequiredFields(entry: VaultEntry): String?
    fun validateFieldContent(entry: VaultEntry): String?
    fun getSensitiveFields(): Set<String>
    fun extractSummary(entry: VaultEntry): String
    fun suggestedCategory(): String
    fun supportsAutofill(): Boolean
    fun initializeDefaults(entry: VaultEntry): VaultEntry = entry
    fun cleanup(entry: VaultEntry): VaultEntry = entry
}