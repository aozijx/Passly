package com.aozijx.passly.core.autofill.model

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultIconable
import com.aozijx.passly.domain.entry.model.lookup.MatchType

/**
 * 已解析的候选凭据：裁剪后的 UI 安全子集 + 可选的二阶段 Fill 字段。
 *
 * 不包含 notes、customFields、attachments、SSH key、credit card 等敏感字段，
 * 阶段一候选展示默认不包含密码/OTP。只有用户选择填充并通过二阶段验证后，
 * 才允许携带实际填充值。
 *
 * 遵循最小暴露原则（Principle of Least Exposure）。
 *
 * 转换统一由 [com.aozijx.passly.core.autofill.pipeline.CandidateResolver] 负责，
 * 不再提供 VaultEntry.toResolvedCandidate() 扩展。
 */
data class ResolvedCandidate(
    /** 数据库条目 ID */
    val candidateId: String,
    /** 用户可见标题 */
    val displayName: String,
    /** 用户名，来自低敏展示数据。 */
    val username: String,
    /** 二阶段填充时才应出现的密码；阶段一候选必须保持为空。 */
    val password: String,
    /** 二阶段填充时才应出现的 TOTP Code（若可用）。 */
    val totpCode: String? = null,
    /** 关联域名（用于 PendingIntent 构建） */
    override val associatedDomain: String? = null,
    /** 关联应用包名 */
    override val associatedAppPackage: String? = null,
    /** 字段到值的映射：key = FieldRole，value = 待填内容 */
    val fields: Map<FieldRole, String> = emptyMap(),
    override val iconName: String? = null,
    override val iconCustomPath: String? = null,
    override val entryType: EntryType = EntryType.LOGIN,
    /** 匹配类型（仅 Pipeline 路径有值，BottomSheet 直接查阅路径为 null） */
    val matchedBy: MatchType? = null,
    val matchedPackage: String? = null,
    val matchedDomain: String? = null,
) : VaultIconable
