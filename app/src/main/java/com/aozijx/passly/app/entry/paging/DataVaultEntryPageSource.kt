package com.aozijx.passly.app.entry.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DataVaultEntryPageSource @Inject constructor(
    private val store: EntryPagingStore,
) : VaultEntryPageSource {
    override fun pages(
        query: EntryListQuery,
        config: PagingConfig,
    ): Flow<PagingData<EntryListItem>> = store.pages(query, config)
}
