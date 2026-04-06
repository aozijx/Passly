package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

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

    // UI 表现层相关：重写接口方法
    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "基本信息", fields = listOf(
                    FieldDefinition("title", "标题", isRequired = true),
                    FieldDefinition("username", "用户名", isRequired = true),
                    FieldDefinition(
                        "password",
                        "密码",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.PASSWORD
                    ),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "额外信息", fields = listOf(
                    FieldDefinition("uriList", "网址", fieldType = FieldType.URL),
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            ), FieldGroup(
                title = "安全设置", fields = listOf(
                    FieldDefinition("favorite", "收藏", fieldType = FieldType.TOGGLE)
                )
            )
        )
    }
}
