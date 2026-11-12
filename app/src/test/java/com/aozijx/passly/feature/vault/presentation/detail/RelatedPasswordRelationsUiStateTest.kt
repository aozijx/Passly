package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class RelatedPasswordRelationsUiStateTest {
    @Test
    fun `query is explicit for every picker state`() {
        assertEquals("", RelatedPasswordBindingUiState.Closed.query)
        assertEquals("", RelatedPasswordBindingUiState.Loading.query)
        assertEquals("", RelatedPasswordBindingUiState.LoadFailed.query)
        assertEquals("git", RelatedPasswordBindingUiState.Ready(emptyList(), query = "git").query)
    }
}
