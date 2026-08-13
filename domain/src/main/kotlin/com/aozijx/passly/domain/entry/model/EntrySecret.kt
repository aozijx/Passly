package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.CustomField
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
import com.aozijx.passly.domain.entry.model.secret.PasskeySecret
import com.aozijx.passly.domain.entry.model.secret.SshSecret
import com.aozijx.passly.domain.entry.model.secret.WifiSecret

/**
 * 单个条目的凭据数据。
 *
 * `login`、`card`、`identity`、`ssh`、`wifi`、`passkey`、`otp`
 * 中最多只能有一个，与 EntryType 对应。备注和自定义字段是该原子凭据的扩展。
 * 同一账户的 Login、OTP、Passkey 等能力应拆成多个 Entry，并通过类型化 EntryLink
 * 关联到一个不含敏感 payload 的 ACCOUNT Entry。
 */
data class EntrySecret(
    /** 登录凭据：用户名/邮箱 + 密码。 */
    val login: LoginSecret? = null,

    /** 纯文本备注。 */
    val notes: String? = null,

    /** 支付卡片：卡号、有效期、CVV、持卡人、PIN。 */
    val card: CardSecret? = null,

    /** 身份信息：证件号、安全问题、助记词、恢复码。 */
    val identity: IdentitySecret? = null,

    /** SSH 密钥：私钥/公钥/口令。 */
    val ssh: SshSecret? = null,

    /** Wi-Fi 凭据：密码、安全类型、是否隐藏。 */
    val wifi: WifiSecret? = null,

    /** Passkey：凭据ID、RP ID、用户句柄、私钥引用。 */
    val passkey: PasskeySecret? = null,

    /** OTP 配置（TOTP / HOTP / Steam）。 */
    val otp: OtpSecret? = null,

    /** 自定义字段列表。 */
    val customFields: List<CustomField> = emptyList()
)
