package com.aozijx.passly.feature.vault.entry

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.app.entry.paging.DataVaultDataChangeSignal
import com.aozijx.passly.app.entry.paging.DataVaultEntryPageSource
import com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier
import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VaultDataAdaptersTest {
    @Test
    fun pageSourceForwardsQueryConfigAndFlowIdentity() {
        val expectedFlow = flowOf(PagingData.empty<EntryListItem>())
        var receivedQuery: EntryListQuery? = null
        var receivedConfig: PagingConfig? = null
        val store = object : EntryPagingStore {
            override fun pages(
                query: EntryListQuery,
                config: PagingConfig,
            ): Flow<PagingData<EntryListItem>> {
                receivedQuery = query
                receivedConfig = config
                return expectedFlow
            }
        }
        val adapter = DataVaultEntryPageSource(store)
        val query = EntryListQuery()
        val config = PagingConfig(pageSize = 23)

        val actualFlow = adapter.pages(query, config)

        assertEquals(query, receivedQuery)
        assertSame(config, receivedConfig)
        assertSame(expectedFlow, actualFlow)
    }

    @Test
    fun changeSignalExposesNotifierFlowWithoutTransformation() {
        val notifier = EntryDataRefreshNotifier()
        val adapter = DataVaultDataChangeSignal(notifier)

        assertSame(notifier.events, adapter.changes())
    }
}
