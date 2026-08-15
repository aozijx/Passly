package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldDefinition
import com.aozijx.passly.domain.entry.model.FieldKey

/**
 * 默认条目类型策略实现。
 *
 * 通过内部策略配置映射 [EntryType] → 策略属性。
 * 任何未显式配置的类型都会获得保守的默认值（不支持 Autofill、空分类、空敏感字段、空摘要）。
 */
class DefaultEntryTypePolicy : EntryTypePolicy {

    private data class PolicyConfig(
        val supportsAutofill: Boolean = false,
        val suggestedCategory: String = "",
        val summaryExtractor: (Entry) -> String = { "" }
    )

    private val configs: Map<EntryType, PolicyConfig> = mapOf(
        EntryType.ACCOUNT to PolicyConfig(
            suggestedCategory = "账户",
            summaryExtractor = { "关联账户" }
        ),
        EntryType.LOGIN to PolicyConfig(
            supportsAutofill = true,
            suggestedCategory = "账户",
            summaryExtractor = { entry -> entry.profile.associations.domains.firstOrNull() ?: "无网址" }
        ),
        EntryType.OTP to PolicyConfig(
            suggestedCategory = "认证",
            summaryExtractor = { entry ->
                val config = entry.secret.otp?.config
                "${config?.digits ?: 6} 位 / ${config?.periodSeconds ?: 30}s"
            }
        ),
        EntryType.SEED_PHRASE to PolicyConfig(
            suggestedCategory = "加密",
            summaryExtractor = { "12/24 词" }
        ),
        EntryType.RECOVERY_CODE to PolicyConfig(
            suggestedCategory = "认证",
            summaryExtractor = { "恢复码" }
        ),
        EntryType.PASSKEY to PolicyConfig(
            suggestedCategory = "认证",
            summaryExtractor = { "Passkey" }
        ),
        EntryType.SSH_KEY to PolicyConfig(
            suggestedCategory = "技术",
            summaryExtractor = { entry -> entry.profile.associations.domains.firstOrNull() ?: "无主机" }
        ),
        EntryType.WIFI to PolicyConfig(
            supportsAutofill = true,
            suggestedCategory = "网络",
            summaryExtractor = { entry ->
                "加密类型 ${entry.secret.wifi?.securityType ?: "WPA/WPA2"}"
            }
        ),
        EntryType.BANK_CARD to PolicyConfig(
            suggestedCategory = "金融",
            summaryExtractor = { entry ->
                val lastFour =
                    entry.secret.card?.cardNumber.orEmpty().takeLast(4)
                "\u2022\u2022${lastFour}"
            }
        ),
        EntryType.ID_CARD to PolicyConfig(
            suggestedCategory = "身份",
            summaryExtractor = { "证件信息" }
        )
    )

    override fun supportsAutofill(type: EntryType): Boolean =
        configs[type]?.supportsAutofill ?: false

    override fun suggestedCategory(type: EntryType): String =
        configs[type]?.suggestedCategory ?: ""

    override fun sensitiveFields(type: EntryType): Set<FieldKey> =
        EntryTypeDefinitions[type].fields
            .filter { it.access != EntryFieldAccess.SUMMARY }
            .mapTo(linkedSetOf(), EntryFieldDefinition::key)

    override fun extractSummary(type: EntryType, entry: Entry): String =
        configs[type]?.summaryExtractor?.invoke(entry) ?: ""
}
