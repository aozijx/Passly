package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntrySecret
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

    private fun getCommonFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val secret = entry.secret
        return when (key) {
            FieldKey.TITLE -> entry.title
            FieldKey.USERNAME -> entry.username
            FieldKey.PASSWORD -> when (secret) {
                is EntrySecret.Login -> secret.data.password
                is EntrySecret.Wifi -> secret.data.password
                is EntrySecret.SshKey -> secret.data.passphrase
                else -> null
            }

            FieldKey.EMAIL -> (secret as? EntrySecret.Login)?.data?.email
            FieldKey.NOTES -> when (secret) {
                is EntrySecret.Login -> secret.data.notes
                is EntrySecret.Note -> secret.notes
                is EntrySecret.VaultData -> secret.notes
                else -> null
            }

            FieldKey.URIS -> entry.website?.matchDomains?.joinToString(", ")
            else -> null
        }
    }

    private fun getTotpFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val otp = (entry.secret as? EntrySecret.Otp)?.data
        return when (key) {
            FieldKey.TOTP_SECRET -> otp?.config?.secret
            FieldKey.TOTP_ISSUER -> otp?.config?.issuer
            FieldKey.TOTP_PERIOD -> (otp?.config?.periodSeconds ?: 30).toString()
            FieldKey.TOTP_DIGITS -> (otp?.config?.digits ?: 6).toString()
            FieldKey.TOTP_ALGORITHM -> otp?.config?.algorithm?.name ?: "SHA1"
            else -> null
        }
    }

    private fun getCryptoFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val secret = entry.secret
        return when (key) {
            FieldKey.PASSKEY_DATA -> (secret as? EntrySecret.Passkey)?.data?.privateKeyReference
            FieldKey.RECOVERY_CODES -> (secret as? EntrySecret.Identity)?.data?.recoveryCodes?.joinToString(
                "\n"
            )

            FieldKey.HARDWARE_INFO -> (secret as? EntrySecret.Passkey)?.data?.hardwareKeyInfo
            FieldKey.SSH_KEY -> (secret as? EntrySecret.SshKey)?.data?.privateKey
            FieldKey.SEED_PHRASE -> (secret as? EntrySecret.Identity)?.data?.seedPhrase
            else -> null
        }
    }

    private fun getFinanceFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val card = (entry.secret as? EntrySecret.Card)?.data
        return when (key) {
            FieldKey.CARD_EXPIRATION -> card?.cardExpiry
            FieldKey.CARD_CVV -> card?.cardCvv
            FieldKey.PAYMENT_PIN -> card?.paymentPin
            FieldKey.PAYMENT_PLATFORM -> card?.paymentPlatform
            FieldKey.SECURITY_QUESTION -> (entry.secret as? EntrySecret.Identity)?.data?.securityQuestion
            FieldKey.SECURITY_ANSWER -> (entry.secret as? EntrySecret.Identity)?.data?.securityAnswer
            else -> null
        }
    }

    private fun getIdentityFieldValue(entry: VaultEntry, key: FieldKey): String? = when (key) {
        FieldKey.ID_NUMBER -> (entry.secret as? EntrySecret.Identity)?.data?.idNumber
        else -> null
    }

    private fun getConnectivityFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val wifi = (entry.secret as? EntrySecret.Wifi)?.data
        return when (key) {
            FieldKey.WIFI_SECURITY -> wifi?.securityType
            FieldKey.WIFI_HIDDEN -> if (wifi?.isHidden == true) "\u662F" else "\u5426"
            else -> null
        }
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