package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.feature.vault.model.VaultTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class VaultQueryCoordinator(
    private val entryListQueryRepository: EntryListQueryRepository
) {
    /**
     * 观察条目列表。
     *
     * [refreshTrigger] 为递增计数器，每次刷新自增。
     * 纳入 [QueryParams] 确保 [distinctUntilChanged] 不会过滤连续的刷新请求。
     * 下游 [flatMapLatest] 在刷新值变化时自动取消前一次 Room 订阅，
     * 防止快速连续刷新产生竞态条件。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        debouncedSearchQuery: Flow<String>,
        normalizedSelectedCategory: Flow<String?>,
        distinctSelectedTab: Flow<VaultTab>,
        refreshTrigger: Flow<Long>
    ): Flow<List<EntryListItem>> = combine(
        debouncedSearchQuery, normalizedSelectedCategory, distinctSelectedTab, refreshTrigger
    ) { query, category, tab, refreshId ->
        QueryParams(query = query, category = category, tab = tab, refreshId = refreshId)
    }.distinctUntilChanged().flatMapLatest { params ->
        entryListQueryRepository.observe(
            query = params.query,
            category = params.category,
            filter = params.tab.entryFilter
        )
    }

    private data class QueryParams(
        val query: String,
        val category: String?,
        val tab: VaultTab,
        val refreshId: Long
    )
}
