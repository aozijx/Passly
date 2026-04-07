package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.VaultSummary
import kotlinx.coroutines.flow.Flow

/**
 * 搜索与查询仓库：专门负责列表展示、分类过滤及摘要查询 (Read-Only 优化)
 */
interface VaultSearchRepository {

    /**
     * 条目筛选类型
     */
    enum class EntryFilter {
        ALL,            // 全部
        PASSWORD_ONLY,  // 仅限密码类（非TOTP）
        TOTP_ONLY       // 仅限动态验证码类
    }

    val allCategories: Flow<List<String>>

    /**
     * 按需观察条目摘要（支持搜索、分类和类型过滤的组合查询）
     * 核心方法，接管了原本分散的全量、分类和搜索功能
     */
    fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<VaultSummary>>

    /**
     * 按类型过滤获取存在的分类列表
     */
    fun getCategoriesByFilter(filter: EntryFilter): Flow<List<String>>
}
