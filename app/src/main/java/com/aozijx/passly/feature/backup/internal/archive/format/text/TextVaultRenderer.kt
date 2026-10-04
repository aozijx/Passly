package com.aozijx.passly.feature.backup.internal.archive.format.text

import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文本导出渲染器。
 *
 * 将备份数据渲染为人类可读的纯文本格式。
 * 每个条目由分隔线分隔，每个字段一行。
 * 规则：
 * - null、空字符串、空集合直接跳过
 * - 每行只表达一个字段
 * - 多行 Notes 的后续行缩进
 * - 自定义字段输出为 "自定义字段.<名称>: <值>"
 */
@Singleton
class TextVaultRenderer @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    fun render(
        entries: List<BackupEntryRecord>,
        resources: List<BackupResourceRecord> = emptyList(),
        options: TextExportOptions = TextExportOptions()
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Passly 文本导出")
        sb.appendLine("导出时间: ${formatTimestamp(System.currentTimeMillis())}")
        sb.appendLine("条目数量: ${entries.size}")
        sb.appendLine()

        entries.forEachIndexed { index, entry ->
            if (index > 0) sb.appendLine()
            sb.appendLine("----------------------------------------")
            renderEntry(sb, entry, options)
        }

        if (resources.isNotEmpty() && options.includeTechnicalInfo) {
            sb.appendLine()
            sb.appendLine("附件:")
            resources.forEach { resource ->
                sb.appendLine(
                    "  ${resource.fileName ?: resource.id} (${resource.mimeType ?: "未知类型"}, ${
                        formatSize(
                            resource.size
                        )
                    })"
                )
            }
        }

        return sb.toString()
    }

    private fun renderEntry(
        sb: StringBuilder,
        entry: BackupEntryRecord,
        options: TextExportOptions
    ) {
        val fields = entry.fields.associate { it.key to it.value }
        appendField(sb, "标题", fields["title"]?.text)
        appendField(sb, "类型", entry.type)
        entry.fields.forEach { field ->
            if (field.key == "title" || field.key == "notes" || field.key == "custom_fields") {
                return@forEach
            }
            appendField(sb, fieldLabel(field.key), field.value.toDisplayText())
        }
        fields["custom_fields"]?.customFields.orEmpty().forEach { field ->
            appendField(sb, "自定义字段.${field.name}", field.value)
        }
        fields["notes"]?.text?.let { notes ->
            val lines = notes.lines()
            if (lines.size == 1) {
                appendField(sb, "备注", notes)
            } else {
                sb.appendLine("备注: ${lines.first()}")
                lines.drop(1).take(options.maxNotesLines - 1).forEach { line ->
                    sb.appendLine("      $line")
                }
                if (lines.size > options.maxNotesLines) {
                    sb.appendLine("      ...（共 ${lines.size} 行）")
                }
            }
        }

        // 技术信息（仅在显式开启时输出）
        if (options.includeTechnicalInfo) {
            appendField(sb, "条目 ID", entry.id)
            appendField(sb, "版本", entry.revision.toString())
            appendField(sb, "创建时间", formatTimestamp(entry.createdAt))
            appendField(sb, "更新时间", formatTimestamp(entry.updatedAt))
        }
    }

    private fun BackupFieldValue.toDisplayText(): String? = when (kind) {
        BackupFieldValue.KIND_TEXT -> text
        BackupFieldValue.KIND_TEXT_LIST -> texts?.joinToString(", ")
        BackupFieldValue.KIND_BOOLEAN -> booleanValue?.toString()
        BackupFieldValue.KIND_INTEGER -> integerValue?.toString()
        BackupFieldValue.KIND_LONG -> longValue?.toString()
        BackupFieldValue.KIND_CUSTOM_FIELDS -> null
        else -> null
    }

    private fun fieldLabel(key: String): String = FIELD_LABELS[key] ?: key

    private fun appendField(sb: StringBuilder, label: String, value: String?) {
        if (value.isNullOrBlank()) return
        sb.appendLine("$label: $value")
    }

    private fun formatTimestamp(epochMillis: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMillis))

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }

    private companion object {
        val FIELD_LABELS = mapOf(
            "username" to "用户名",
            "email" to "邮箱",
            "password" to "密码",
            "primary_url" to "网址",
            "tags" to "标签",
            "otp_type" to "OTP 类型",
            "otp_secret" to "OTP 密钥",
            "otp_encoding" to "OTP 编码",
            "otp_issuer" to "OTP 发行者",
            "otp_account_name" to "OTP 账户",
            "card_number" to "卡号",
            "card_expiration" to "有效期",
            "card_cvv" to "CVV",
            "card_holder" to "持卡人",
            "payment_platform" to "支付平台",
            "id_number" to "身份证号",
            "security_question" to "安全问题",
            "security_answer" to "安全答案",
            "seed_phrase" to "种子短语",
            "recovery_codes" to "恢复码",
            "ssh_public_key" to "SSH 公钥",
            "ssh_passphrase" to "SSH 密码短语",
            "wifi_security" to "安全类型",
        )
    }
}
