package com.aozijx.passly.core.autofill.model

import com.aozijx.passly.domain.model.entry.VaultIconable
import com.aozijx.passly.domain.model.lookup.MatchType

/**
 * 已解析的候选凭据：裁剪后的 UI 安全子集 + Fill 所需的全部字段。
 *
 * 不包含 notes、customFields、attachments、SSH key、credit card 等敏感字段，
 * 仅暴露 BottomSheet 展示 & ResponseFactory 填充所需的最小集合。
 *
 * 遵循最小暴露原则（Principle of Least Exposure）。
 *
 * 转换统一由 [com.aozijx.passly.core.autofill.pipeline.CandidateResolver] 负责，
 * 不再提供 VaultEntry.toResolvedCandidate() 扩展。
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
    override val associatedDomain: String? = null,
    /** 关联应用包名 */
    override val associatedAppPackage: String? = null,
    /** 副标题（匹配类型、使用频次等上下文信息） */
    val subtitle: String = "",
    /** 字段到值的映射：key = FieldRole，value = 待填内容 */
    val fields: Map<FieldRole, String> = emptyMap(),
    override val iconName: String? = null,
    override val iconCustomPath: String? = null,
    val entryType: String = "",
    /** 匹配类型（仅 Pipeline 路径有值，BottomSheet 直接查阅路径为 null） */
    val matchedBy: MatchType? = null,
    val matchedPackage: String? = null,
    val matchedDomain: String? = null,
) : VaultIconable {
    override val category: String get() = ""
}
