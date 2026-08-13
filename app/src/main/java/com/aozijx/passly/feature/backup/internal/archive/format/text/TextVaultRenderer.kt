package com.aozijx.passly.feature.backup.internal.archive.format.text

import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
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
        appendField(sb, "标题", entry.summary.title)
        appendField(sb, "类型", entry.type)

        // 登录字段
        entry.secret.login?.let { login ->
            appendField(sb, "用户名", entry.summary.username)
            appendField(sb, "邮箱", login.email)
            appendField(sb, "密码", login.password)
        }

        // 网址
        entry.summary.website?.let { website ->
            appendField(sb, "网址", website.primaryUrl)
        }

        // 标签
        if (entry.summary.tags.isNotEmpty()) {
            appendField(sb, "标签", entry.summary.tags.joinToString(", "))
        }

        // OTP
        entry.secret.otp?.config?.let { otp ->
            appendField(sb, "OTP 类型", otp.type.name)
            appendField(sb, "OTP 密钥", otp.secret)
            appendField(sb, "OTP 编码", otp.encoding.name)
            if (otp.issuer != null) appendField(sb, "OTP 发行者", otp.issuer)
            if (otp.accountName != null) appendField(sb, "OTP 账户", otp.accountName)
        }

        // 卡片字段
        entry.secret.card?.let { card ->
            appendField(sb, "卡号", card.cardNumber)
            appendField(sb, "有效期", card.cardExpiry)
            appendField(sb, "CVV", card.cardCvv)
            appendField(sb, "持卡人", card.cardHolder)
            card.paymentPlatform?.let { appendField(sb, "支付平台", it) }
        }

        // 身份字段
        entry.secret.identity?.let { identity ->
            appendField(sb, "身份证号", identity.idNumber)
            appendField(sb, "安全问题", identity.securityQuestion)
            appendField(sb, "安全答案", identity.securityAnswer)
            appendField(sb, "种子短语", identity.seedPhrase)
            if (identity.recoveryCodes.isNotEmpty()) {
                appendField(sb, "恢复码", identity.recoveryCodes.joinToString(", "))
            }
        }

        // SSH 字段
        entry.secret.ssh?.let { ssh ->
            if (ssh.publicKey != null) appendField(sb, "SSH 公钥", ssh.publicKey.take(64) + "...")
            appendField(sb, "SSH 密码短语", ssh.passphrase)
        }

        // Wi-Fi 字段
        entry.secret.wifi?.let { wifi ->
            appendField(sb, "Wi-Fi 密码", wifi.password)
            wifi.securityType?.let { appendField(sb, "安全类型", it) }
        }

        // 自定义字段
        entry.secret.customFields.forEach { field ->
            appendField(sb, "自定义字段.${field.name}", field.value)
        }

        // 备注
        entry.secret.notes?.let { notes ->
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
            appendField(sb, "版本", entry.version.toString())
            appendField(sb, "创建时间", formatTimestamp(entry.createdAt))
            appendField(sb, "更新时间", formatTimestamp(entry.updatedAt))
        }
    }

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
}
