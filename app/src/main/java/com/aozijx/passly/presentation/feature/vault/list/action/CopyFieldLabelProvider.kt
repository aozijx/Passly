package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.entry.model.FieldKey

/**
 * 将领域字段映射为复制提示中的简短标签。
 */
object CopyFieldLabelProvider {

    /**
     * 根据 [FieldKey] 获取复制操作时的提示标签。
     */
    fun getCopyLabel(key: FieldKey): String = when (key) {
        FieldKey.PASSWORD -> "密码"
        FieldKey.USERNAME -> "账号"
        FieldKey.EMAIL -> "邮箱"
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
}
