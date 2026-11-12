package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryLoadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailHistoryListUiStateTest {
    @Test
    fun `empty result requires both sources to be ready`() {
        assertTrue(historyList().isEmptyResult)
        assertFalse(historyList(activity = DetailHistoryLoadState.LOADING).isEmptyResult)
        assertFalse(historyList(revisions = DetailHistoryLoadState.FAILED).isEmptyResult)
    }

    private fun historyList(
        activity: DetailHistoryLoadState = DetailHistoryLoadState.READY,
        revisions: DetailHistoryLoadState = DetailHistoryLoadState.READY,
    ) = DetailHistoryListUiState(
        filter = DetailHistoryFilter.ALL,
        items = emptyList(),
        activitySource = activity,
        revisionSource = revisions,
    )
}
