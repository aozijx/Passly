package com.aozijx.passly.domain.entry.port

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import kotlinx.coroutines.flow.Flow

/** Streams queried vault entries as pages without exposing persistence details to callers. */
interface EntryPagingRepository {
    fun pages(
        query: EntryListQuery,
        config: PagingConfig,
    ): Flow<PagingData<EntryListItem>>
}
