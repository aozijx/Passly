package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import kotlinx.coroutines.flow.Flow

/**
 * 搜索与查询仓库：专门负责列表展示、分类过滤及摘要查询 (Read-Only 优化)
 */
interface VaultSearchRepository {
    val allEntrySummaries: Flow<List<VaultSummary>>
    val allCategories: Flow<List<String>>

    fun getEntriesByCategory(category: String): Flow<List<VaultEntry>>
    fun getEntrySummariesByCategory(category: String): Flow<List<VaultSummary>>
    fun searchEntries(query: String): Flow<List<VaultEntry>>
    fun searchEntrySummaries(query: String): Flow<List<VaultSummary>>
}
