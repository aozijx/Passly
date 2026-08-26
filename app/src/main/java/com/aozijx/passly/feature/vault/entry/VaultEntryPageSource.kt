package com.aozijx.passly.feature.vault.entry

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import kotlinx.coroutines.flow.Flow

interface VaultEntryPageSource {
    fun pages(
        query: EntryListQuery,
        config: PagingConfig,
    ): Flow<PagingData<EntryListItem>>
}
