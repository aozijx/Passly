package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class VaultQueryCoordinator(
    private val entryListQueryRepository: EntryListQueryRepository
) {
    /**
     * 观察全部条目。分页标签只负责在内存中派生视图，不再重建 Room 查询，
     * 从而让相邻页面在滑动开始前就拥有可显示的数据。
     *
     * [refreshTrigger] 为递增计数器，每次刷新自增。
     * 纳入 [QueryParams] 确保 [distinctUntilChanged] 不会过滤连续的刷新请求。
     * 下游 [flatMapLatest] 在刷新值变化时自动取消前一次 Room 订阅，
     * 防止快速连续刷新产生竞态条件。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        debouncedSearchQuery: Flow<String>,
        refreshTrigger: Flow<Long>
    ): Flow<List<EntryListItem>> = combine(
        debouncedSearchQuery,
        refreshTrigger
    ) { query, refreshId ->
        QueryParams(query = query, refreshId = refreshId)
    }.distinctUntilChanged().flatMapLatest { params ->
        entryListQueryRepository.observe(
            query = params.query,
            filter = EntryFilter.ALL
        )
    }

    private data class QueryParams(
        val query: String,
        val refreshId: Long
    )
}
