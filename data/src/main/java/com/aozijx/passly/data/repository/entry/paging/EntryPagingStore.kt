package com.aozijx.passly.data.repository.entry.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import kotlinx.coroutines.flow.Flow

/** Paging boundary exposed to the application layer without leaking Paging into Domain. */
interface EntryPagingStore {
    fun pages(
        query: EntryListQuery,
        config: PagingConfig,
    ): Flow<PagingData<EntryListItem>>
}
