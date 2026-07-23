package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认字段读取器。
 *
 * 根据 [FieldKey] 从 [VaultEntry] 中提取原始数据值，处理逻辑对所有条目类型通用。
 * 提取逻辑继承自原有的 [com.aozijx.passly.domain.strategy.EntryTypeStrategy] 中的 getFieldValue 实现。
 */
@Singleton
class DefaultEntryFieldReader @Inject constructor() : EntryFieldReader {

    override fun getFieldValue(entry: VaultEntry, key: FieldKey): String? {
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

    // --- 分组提取逻辑 ---

    private fun getCommonFieldValue(entry: VaultEntry, key: FieldKey): String? {
        val secret = entry.secret
        return when (key) {
            FieldKey.TITLE -> entry.summary.title
            FieldKey.USERNAME -> entry.summary.username
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

            FieldKey.URIS -> entry.summary.website?.matchDomains?.joinToString(", ")
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

    // --- FieldKey 扩展判定 ---

    private fun FieldKey.isCommon() = this in listOf(
        FieldKey.TITLE,
        FieldKey.USERNAME,
        FieldKey.PASSWORD,
        FieldKey.EMAIL,
        FieldKey.NOTES,
        FieldKey.URIS
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
}
