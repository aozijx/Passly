package com.aozijx.passly.feature.vault.presentation.list.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.aozijx.passly.feature.vault.presentation.list.VaultSearchInput

@Stable
class VaultSearchStateHolder internal constructor(
    initialState: VaultSearchState,
    initialSearch: VaultSearchInput,
    private val currentSearch: () -> VaultSearchInput,
    private val onQueryChange: (String) -> Unit,
    private val onSearchActiveChange: (Boolean) -> Unit,
    private val onExpandBars: () -> Unit,
) {
    var uiState by mutableStateOf(initialState)
        private set

    private var previousSearch = initialSearch

    fun synchronize(search: VaultSearchInput) {
        val searchExited = previousSearch.isActive && !search.isActive
        uiState = uiState.synchronize(search)
        if (searchExited || uiState.isEditing) onExpandBars()
        previousSearch = search
    }

    fun focusChanged(focused: Boolean) {
        val nextState = uiState.onFocusChanged(focused, currentSearch().query)
        uiState = nextState
        if (!focused && nextState == VaultSearchState.Browsing && currentSearch().isActive) {
            onSearchActiveChange(false)
        }
    }

    fun submit(query: String) {
        uiState = settle(query)
        if (uiState == VaultSearchState.Browsing) onSearchActiveChange(false)
    }

    fun clear() {
        onQueryChange("")
        if (uiState.isResults) exit()
    }

    fun request() {
        uiState = startEditing()
        onExpandBars()
        onSearchActiveChange(true)
    }

    fun updatePullProgress(progress: Float) {
        uiState = uiState.onPullProgressChanged(progress)
    }

    fun handleBack() {
        if (uiState.isEditing) {
            uiState = settle(currentSearch().query)
            if (uiState == VaultSearchState.Browsing) onSearchActiveChange(false)
        } else {
            exit()
        }
    }

    fun pause() {
        uiState = settle(currentSearch().query)
        if (currentSearch().isActive && uiState == VaultSearchState.Browsing) {
            onSearchActiveChange(false)
        }
    }

    private fun exit() {
        uiState = uiState.synchronize(VaultSearchInput.Closed)
        onExpandBars()
        onSearchActiveChange(false)
    }
}

@Composable
internal fun rememberVaultSearchStateHolder(
    search: VaultSearchInput,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onExpandBars: () -> Unit,
): VaultSearchStateHolder {
    val currentSearch by rememberUpdatedState(search)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearchActiveChange by rememberUpdatedState(onSearchActiveChange)
    val currentOnExpandBars by rememberUpdatedState(onExpandBars)

    return remember {
        VaultSearchStateHolder(
            initialState = VaultSearchState.initial(search),
            initialSearch = search,
            currentSearch = { currentSearch },
            onQueryChange = { currentOnQueryChange(it) },
            onSearchActiveChange = { currentOnSearchActiveChange(it) },
            onExpandBars = { currentOnExpandBars() },
        )
    }
}
