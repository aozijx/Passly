package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy
import com.aozijx.passly.domain.strategy.FieldDefinition
import com.aozijx.passly.domain.strategy.FieldGroup
import com.aozijx.passly.domain.strategy.FieldType

/**
 * 密码类型的业务策略实现
 */
class PasswordEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.PASSWORD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "标题不能为空"
        if (entry.username.isBlank()) return "用户名不能为空"
        if (entry.password.isBlank()) return "密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // 检查是否存在基本的密码安全性
        val passwordLength = entry.password.length
        if (passwordLength < 1) return "密码为空"
        // 可以添加更复杂的密码强度检查
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password", "username")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.uriList?.firstOrNull() ?: "无网址"
    }

    override fun suggestedCategory(): String = "账户"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory()
        )
    }

    // UI 表现层相关
    fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "基本信息",
                fields = listOf(
                    FieldDefinition("title", "标题", isRequired = true),
                    FieldDefinition("username", "用户名", isRequired = true),
                    FieldDefinition("password", "密码", isSensitive = true, isRequired = true, fieldType = FieldType.PASSWORD),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ),
            FieldGroup(
                title = "额外信息",
                fields = listOf(
                    FieldDefinition("uriList", "网址", fieldType = FieldType.URL),
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            ),
            FieldGroup(
                title = "安全设置",
                fields = listOf(
                    FieldDefinition("favorite", "收藏", fieldType = FieldType.TOGGLE)
                )
            )
        )
    }
}

/**
 * WiFi 类型的业务策略实现
 */
class WiFiEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.WIFI

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "WiFi 名称不能为空"
        if (entry.username.isBlank()) return "SSID 不能为空"
        if (entry.password.isBlank()) return "密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // WiFi 加密类型验证
        val validEncryption = setOf("WPA", "WPA2", "WPA3", "WEP", "Open")
        if (!validEncryption.contains(entry.wifiEncryptionType)) {
            return "无效的加密类型: ${entry.wifiEncryptionType}"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password", "username")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.wifiEncryptionType ?: "Unknown"
    }

    override fun suggestedCategory(): String = "网络"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            wifiEncryptionType = entry.wifiEncryptionType ?: "WPA2"
        )
    }
}

/**
 * 银行卡类型的业务策略实现
 */
class BankCardEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.BANK_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "银行名称不能为空"
        if (entry.password.isBlank()) return "卡号不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // 卡号基本格式检查（Luhn 算法可选）
        val cardNumber = entry.password.filter { it.isDigit() }
        if (cardNumber.length !in 13..19) {
            return "无效的卡号长度"
        }
        
        // 验证有效期格式
        entry.cardExpiration?.let {
            if (!it.matches(Regex("^\\d{2}/\\d{2}$"))) {
                return "有效期格式应为 MM/YY"
            }
        }
        
        // CVV 格式检查
        entry.cardCvv?.let {
            if (!it.matches(Regex("^\\d{3,4}$"))) {
                return "CVV 应为 3-4 位数字"
            }
        }
        
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password", "username", "cardCvv", "cardExpiration", "paymentPin")
    }

    override fun extractSummary(entry: VaultEntry): String {
        // 显示卡号末四位
        val lastFour = entry.password.takeLast(4)
        return "••${lastFour}"
    }

    override fun suggestedCategory(): String = "金融"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            matchType = 0 // 不启用自动填充匹配
        )
    }
}

/**
 * SSH 密钥类型的业务策略实现
 */
class SshKeyEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.SSH_KEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "标识名不能为空"
        if (entry.sshPrivateKey.isNullOrBlank()) return "SSH 私钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        entry.sshPrivateKey?.let {
            if (!it.contains("BEGIN")) return "无效的 SSH 私钥格式"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("sshPrivateKey", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.uriList?.firstOrNull() ?: "无主机"
    }

    override fun suggestedCategory(): String = "技术"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            notes = entry.notes ?: "SSH 连接凭据"
        )
    }
}

/**
 * 助记词类型的业务策略实现
 */
class SeedPhraseEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.SEED_PHRASE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "钱包名称不能为空"
        if (entry.cryptoSeedPhrase.isNullOrBlank()) return "助记词不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        entry.cryptoSeedPhrase?.let { phrase ->
            val wordCount = phrase.split(Regex("\\s+")).size
            if (wordCount !in setOf(12, 24)) {
                return "助记词应包含 12 或 24 个单词，实际 $wordCount 个"
            }
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("cryptoSeedPhrase", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "12/24 词"
    }

    override fun suggestedCategory(): String = "加密"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            notes = entry.notes ?: "备份好这个助记词，永远不要分享给任何人"
        )
    }
}

/**
 * Passkey 类型的业务策略实现
 */
class PasskeyEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.PASSKEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "Passkey 标题不能为空"
        if (entry.passkeyDataJson.isNullOrBlank()) return "Passkey 数据不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (!entry.recoveryCodes.isNullOrBlank() && entry.recoveryCodes.length < 6) {
            return "恢复码内容异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("passkeyDataJson", "recoveryCodes")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.recoveryCodes.isNullOrBlank()) "Passkey" else "Passkey + 恢复码"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }
}

/**
 * 恢复码类型的业务策略实现
 */
class RecoveryCodeEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.RECOVERY_CODE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "恢复码标题不能为空"
        if (entry.recoveryCodes.isNullOrBlank()) return "恢复码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.recoveryCodes != null && entry.recoveryCodes.length < 4) {
            return "恢复码内容异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("recoveryCodes")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "恢复码"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }
}

/**
 * TOTP 类型的业务策略实现
 */
class TotpEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.TOTP

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "TOTP 标题不能为空"
        if (entry.totpSecret.isNullOrBlank()) return "TOTP 密钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.totpPeriod <= 0) return "TOTP 周期必须大于 0"
        if (entry.totpDigits !in 5..8) return "TOTP 位数应在 5-8 位"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("totpSecret")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "${entry.totpDigits} 位 / ${entry.totpPeriod}s"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }
}

/**
 * 证件类型的业务策略实现
 */
class IdCardEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.ID_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "证件标题不能为空"
        if (entry.idNumber.isNullOrBlank()) return "证件号码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.idNumber != null && entry.idNumber.length < 6) {
            return "证件号码长度异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("idNumber")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.cardExpiration.isNullOrBlank()) "证件信息" else "有效期 ${entry.cardExpiration}"
    }

    override fun suggestedCategory(): String = "身份"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }
}
