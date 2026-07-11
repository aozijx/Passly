package com.aozijx.passly.core.autofill.model

import com.aozijx.passly.domain.model.VaultEntry

/**
 * 已解析的候选凭据：解密后的凭据数据 + 字段映射。
 */
data class ResolvedCandidate(
    /** 数据库条目 ID */
    val candidateId: Int,
    /** 用户可见标题 */
    val displayName: String,
    /** 解密后的用户名 */
    val username: String,
    /** 解密后的密码 */
    val password: String,
    /** 解密后的 TOTP Code（若可用） */
    val totpCode: String? = null,
    /** 关联域名（用于 PendingIntent 构建） */
    val associatedDomain: String? = null,
    /** 副标题（匹配类型、使用频次等上下文信息） */
    val subtitle: String = "",
    /** 字段到值的映射：key = FieldRole，value = 待填内容 */
    val fields: Map<FieldRole, String> = emptyMap(),
    /** 图标标识（可为资源 ID 字符串或 URL） */
    val icon: String? = null,
    /** 是否需要二次认证（如 biometric）后才能完成填充 */
    val needsAuthentication: Boolean = false,
)

fun ResolvedCandidate.toVaultEntry(): VaultEntry = VaultEntry(
    id = candidateId,
    title = displayName,
    username = username,
    password = password,
    associatedDomain = associatedDomain,
    category = "",
)

/**
 * 字段角色枚举：核心层用语义角色描述字段，适配器层再映射到具体 AutofillId。
 */
enum class FieldRole {
    USERNAME,
    PASSWORD,
    OTP,

    /** 提交按钮（部分实现需要"点击提交"触发保存检测） */
    SUBMIT,

    /** 未知角色字段 */
    UNKNOWN,
}