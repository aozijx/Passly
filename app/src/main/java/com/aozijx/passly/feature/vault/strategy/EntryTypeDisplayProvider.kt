package com.aozijx.passly.feature.vault.strategy

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.feature.vault.strategy.EntryTypeDisplayProvider.getCopyLabel

/**
 * 条目类型的 UI 显示信息提供者。
 *
 * 职责范围：
 * - [getCopyLabel]：字段复制操作的提示标签
 *
 * 与 [com.aozijx.passly.domain.strategy.EntryTypeStrategy] 保持对应，
 * Domain 层专注验证、字段提取、敏感字段标识；UI 层负责显示标签和组件类型映射。
 */
object EntryTypeDisplayProvider {

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
