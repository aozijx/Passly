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
 * 条目凭据数据的可组合聚合。
 *
 * 所有字段均为可空或空列表，同一 EntrySecret 可以同时持有
 * 登录凭据 + OTP 配置 + 自定义字段 + 备注，无需互斥。
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
