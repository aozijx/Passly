package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

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
            category = suggestedCategory(), notes = entry.notes ?: "SSH 连接凭据"
        )
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "基本信息", fields = listOf(
                    FieldDefinition("title", "名称", isRequired = true),
                    FieldDefinition("username", "用户名"),
                    FieldDefinition(
                        "sshPrivateKey",
                        "SSH 私钥",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.TEXTAREA
                    ),
                    FieldDefinition(
                        "password",
                        "密钥口令 (Passphrase)",
                        isSensitive = true,
                        fieldType = FieldType.PASSWORD
                    ),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "连接详情", fields = listOf(
                    FieldDefinition("uriList", "主机/地址", fieldType = FieldType.URL),
                    FieldDefinition("sshPublicKey", "SSH 公钥", fieldType = FieldType.TEXTAREA)
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
