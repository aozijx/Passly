package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.model.entry.secret.CardSecret
import com.aozijx.passly.domain.model.entry.secret.CustomField
import com.aozijx.passly.domain.model.entry.secret.IdentitySecret
import com.aozijx.passly.domain.model.entry.secret.LoginSecret
import com.aozijx.passly.domain.model.entry.secret.OtpSecret
import com.aozijx.passly.domain.model.entry.secret.PasskeySecret
import com.aozijx.passly.domain.model.entry.secret.SshSecret
import com.aozijx.passly.domain.model.entry.secret.WifiSecret

/**
 * 条目类型对应的敏感凭据数据。
 *
 * 每个变体对应一组类型安全的凭据字段，消除原先 [VaultCredential] 的全能对象模式。
 */
sealed class EntrySecret {

    /** 登录凭据：用户名/邮箱 + 密码 + 备注。 */
    data class Login(val data: LoginSecret) : EntrySecret()

    /** 记事：纯文本内容。 */
    data class Note(val notes: String) : EntrySecret()

    /** 支付卡片：卡号、有效期、CVV、持卡人、PIN。 */
    data class Card(val data: CardSecret) : EntrySecret()

    /** 身份信息：证件号、安全问题、助记词、恢复码。 */
    data class Identity(val data: IdentitySecret) : EntrySecret()

    /** SSH 密钥：私钥/公钥/口令。 */
    data class SshKey(val data: SshSecret) : EntrySecret()

    /** Wi-Fi 凭据：密码、安全类型、是否隐藏。 */
    data class Wifi(val data: WifiSecret) : EntrySecret()

    /** Passkey：凭据ID、RP ID、用户句柄、私钥引用。 */
    data class Passkey(val data: PasskeySecret) : EntrySecret()

    /** OTP 配置（TOTP / HOTP / Steam）。 */
    data class Otp(val data: OtpSecret) : EntrySecret()

    /**
     * 通用凭据：适用于 PASSPORT、LICENSE、DATABASE、SERVER、API_KEY、
     * CRYPTO_WALLET、BANK_CARD、ID_CARD、SEED_PHRASE、RECOVERY_CODE
     * 等没有独立 Secret 类型的条目。
     */
    data class VaultData(
        val customFields: List<CustomField> = emptyList(),
        val notes: String? = null
    ) : EntrySecret()
}
