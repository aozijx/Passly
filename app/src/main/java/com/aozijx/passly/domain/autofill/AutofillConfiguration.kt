package com.aozijx.passly.domain.autofill

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate

/**
 * 填充配置：集中管理 Legacy Autofill / Modern CredentialManager 共用的策略常量和规则。
 *
 * Legacy 路径和 Modern 路径均依赖此配置，修改此文件前需确认影响两条路径。
 */
object AutofillConfiguration {

    /** 最大候选条目数 */
    const val MAX_CANDIDATES = 5

    /** 支持的自动填充入口类型 */
    val SUPPORTED_ENTRY_TYPES = setOf(
        EntryType.LOGIN,
    )

    /**
     * 排序权重：score（降序） > 收藏 > 使用次数 > 更新时间
     *
     * Repository 负责匹配并计算 score，Dispatcher 通过此比较器排序即可。
     * 新增匹配策略只需在 [com.aozijx.passly.domain.entry.model.lookup.MatchType] 中添加枚举值。
     */
    fun compareCandidates(a: CredentialCandidate, b: CredentialCandidate): Int {
        return compareByDescending<CredentialCandidate> { it.score }
            .thenByDescending { it.entry.favorite }
            .thenByDescending { it.entry.updatedAt }
            .compare(a, b)
    }

    /** 判断给定条目类型是否支持自动填充 */
    fun isAutofillSupported(entryType: EntryType): Boolean {
        return entryType in SUPPORTED_ENTRY_TYPES
    }
}
