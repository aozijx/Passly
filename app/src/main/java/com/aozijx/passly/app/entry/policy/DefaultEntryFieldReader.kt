package com.aozijx.passly.app.entry.policy

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认字段读取器。
 *
 * 根据 [FieldKey] 从 [Entry] 中提取原始数据值，处理逻辑对所有条目类型通用。
 * 提取逻辑继承自原有的 [com.aozijx.passly.domain.strategy.EntryTypeStrategy] 中的 getFieldValue 实现。
 */
@Singleton
class DefaultEntryFieldReader @Inject constructor() : EntryFieldReader {

    override fun getFieldValue(entry: Entry, key: FieldKey): String? {
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

    private fun getCommonFieldValue(entry: Entry, key: FieldKey): String? {
        val secret = entry.secret
        return when (key) {
            FieldKey.TITLE -> entry.profile.title
            FieldKey.USERNAME -> entry.profile.username
            FieldKey.PASSWORD -> secret.login?.password
                ?: secret.wifi?.password
                ?: secret.ssh?.passphrase

            FieldKey.EMAIL -> secret.login?.email
            FieldKey.NOTES -> secret.notes

            FieldKey.URIS -> entry.profile.associations.domains.joinToString(", ")
            else -> null
        }
    }

    private fun getTotpFieldValue(entry: Entry, key: FieldKey): String? {
        val otp = entry.secret.otp
        return when (key) {
            FieldKey.OTP_SECRET -> otp?.config?.secret
            FieldKey.OTP_ISSUER -> otp?.config?.issuer
            FieldKey.OTP_ACCOUNT_NAME -> otp?.config?.accountName
            FieldKey.OTP_PERIOD -> (otp?.config?.periodSeconds ?: 30).toString()
            FieldKey.OTP_COUNTER -> otp?.config?.counter?.toString()
            FieldKey.OTP_DIGITS -> (otp?.config?.digits ?: 6).toString()
            FieldKey.OTP_ALGORITHM -> otp?.config?.algorithm?.name ?: "SHA1"
            else -> null
        }
    }

    private fun getCryptoFieldValue(entry: Entry, key: FieldKey): String? {
        val secret = entry.secret
        return when (key) {
            FieldKey.PASSKEY_DATA -> secret.passkey?.privateKeyReference
            FieldKey.RECOVERY_CODES -> secret.identity?.recoveryCodes?.joinToString(
                "\n"
            )

            FieldKey.HARDWARE_INFO -> secret.passkey?.hardwareKeyInfo
            FieldKey.SSH_KEY -> secret.ssh?.privateKey
            FieldKey.SEED_PHRASE -> secret.identity?.seedPhrase
            else -> null
        }
    }

    private fun getFinanceFieldValue(entry: Entry, key: FieldKey): String? {
        val card = entry.secret.card
        return when (key) {
            FieldKey.CARD_EXPIRATION -> card?.cardExpiry
            FieldKey.CARD_CVV -> card?.cardCvv
            FieldKey.PAYMENT_PIN -> card?.paymentPin
            FieldKey.PAYMENT_PLATFORM -> card?.paymentPlatform
            FieldKey.SECURITY_QUESTION -> entry.secret.identity?.securityQuestion
            FieldKey.SECURITY_ANSWER -> entry.secret.identity?.securityAnswer
            else -> null
        }
    }

    private fun getIdentityFieldValue(entry: Entry, key: FieldKey): String? = when (key) {
        FieldKey.ID_NUMBER -> entry.secret.identity?.idNumber
        else -> null
    }

    private fun getConnectivityFieldValue(entry: Entry, key: FieldKey): String? {
        val wifi = entry.secret.wifi
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
