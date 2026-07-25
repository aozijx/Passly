package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认条目类型策略实现。
 *
 * 通过内部策略配置映射 [EntryType] → 策略属性。
 * 任何未显式配置的类型都会获得保守的默认值（不支持 Autofill、空分类、空敏感字段、空摘要）。
 */
@Singleton
class DefaultEntryTypePolicy @Inject constructor() : EntryTypePolicy {

    private data class PolicyConfig(
        val supportsAutofill: Boolean = false,
        val suggestedCategory: String = "",
        val sensitiveFields: Set<String> = emptySet(),
        val summaryExtractor: (VaultEntry) -> String = { "" }
    )

    private val configs: Map<EntryType, PolicyConfig> = mapOf(
        EntryType.LOGIN to PolicyConfig(
            supportsAutofill = true,
            suggestedCategory = "账户",
            sensitiveFields = setOf("password", "username", "totpSecret"),
            summaryExtractor = { entry -> entry.website?.matchDomains?.firstOrNull() ?: "无网址" }
        ),
        EntryType.TOTP to PolicyConfig(
            suggestedCategory = "认证",
            sensitiveFields = setOf("totpSecret"),
            summaryExtractor = { entry ->
                val config = entry.secret.otp?.config
                "${config?.digits ?: 6} 位 / ${config?.periodSeconds ?: 30}s"
            }
        ),
        EntryType.SEED_PHRASE to PolicyConfig(
            suggestedCategory = "加密",
            sensitiveFields = setOf("seedPhrase", "password"),
            summaryExtractor = { "12/24 词" }
        ),
        EntryType.RECOVERY_CODE to PolicyConfig(
            suggestedCategory = "认证",
            sensitiveFields = setOf("recoveryCodes"),
            summaryExtractor = { "恢复码" }
        ),
        EntryType.PASSKEY to PolicyConfig(
            suggestedCategory = "认证",
            sensitiveFields = setOf("passkeyPrivateKeyReference", "recoveryCodes"),
            summaryExtractor = { "Passkey" }
        ),
        EntryType.SSH_KEY to PolicyConfig(
            suggestedCategory = "技术",
            sensitiveFields = setOf("sshPrivateKey", "password"),
            summaryExtractor = { entry -> entry.website?.matchDomains?.firstOrNull() ?: "无主机" }
        ),
        EntryType.WIFI to PolicyConfig(
            supportsAutofill = true,
            suggestedCategory = "网络",
            sensitiveFields = setOf("password"),
            summaryExtractor = { entry ->
                "加密类型 ${entry.secret.wifi?.securityType ?: "WPA/WPA2"}"
            }
        ),
        EntryType.BANK_CARD to PolicyConfig(
            suggestedCategory = "金融",
            sensitiveFields = setOf(
                "password",
                "username",
                "cardCvv",
                "cardExpiration",
                "paymentPin",
                "securityAnswer"
            ),
            summaryExtractor = { entry ->
                val lastFour =
                    entry.secret.card?.cardNumber.orEmpty().takeLast(4)
                "\u2022\u2022${lastFour}"
            }
        ),
        EntryType.ID_CARD to PolicyConfig(
            suggestedCategory = "身份",
            sensitiveFields = setOf("idNumber"),
            summaryExtractor = { "证件信息" }
        )
    )

    override fun supportsAutofill(type: EntryType): Boolean =
        configs[type]?.supportsAutofill ?: false

    override fun suggestedCategory(type: EntryType): String =
        configs[type]?.suggestedCategory ?: ""

    override fun sensitiveFields(type: EntryType): Set<String> =
        configs[type]?.sensitiveFields ?: emptySet()

    override fun extractSummary(type: EntryType, entry: VaultEntry): String =
        configs[type]?.summaryExtractor?.invoke(entry) ?: ""
}
