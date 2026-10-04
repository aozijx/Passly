package com.aozijx.passly.presentation.feature.vault.list.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

@Stable
class VaultSearchStateHolder internal constructor(
    initialState: VaultSearchState,
    initialSearchActive: Boolean,
    private val currentQuery: () -> String,
    private val currentSearchActive: () -> Boolean,
    private val onQueryChange: (String) -> Unit,
    private val onSearchActiveChange: (Boolean) -> Unit,
    private val onExpandBars: () -> Unit,
) {
    var uiState by mutableStateOf(initialState)
        private set

    private var wasSearchActive = initialSearchActive

    fun synchronize(searchActive: Boolean, query: String) {
        val searchExited = wasSearchActive && !searchActive
        uiState = uiState.synchronize(searchActive, query)
        if (searchExited || uiState.isEditing) onExpandBars()
        wasSearchActive = searchActive
    }

    fun queryChanged(query: String) = onQueryChange(query)

    fun focusChanged(focused: Boolean) {
        val nextState = uiState.onFocusChanged(focused, currentQuery())
        uiState = nextState
        when {
            focused && !currentSearchActive() -> {
                onExpandBars()
                onSearchActiveChange(true)
            }
            !focused &&
                nextState.phase == VaultSearchPhase.BROWSING &&
                currentSearchActive() -> onSearchActiveChange(false)
        }
    }

    fun submit(query: String) {
        uiState = uiState.settle(query)
        if (query.isBlank()) onSearchActiveChange(false)
    }

    fun clear() {
        onQueryChange("")
        if (uiState.phase == VaultSearchPhase.RESULTS) exit()
    }

    fun request() {
        uiState = uiState.startEditing()
        onExpandBars()
        onSearchActiveChange(true)
    }

    fun updatePullProgress(progress: Float) {
        uiState = uiState.onPullProgressChanged(progress)
    }

    fun handleBack() {
        if (uiState.isEditing) {
            uiState = uiState.settle(currentQuery())
            if (currentQuery().isBlank()) onSearchActiveChange(false)
        } else {
            exit()
        }
    }

    fun pause() {
        uiState = uiState.onScreenPaused(currentQuery())
        if (currentSearchActive() && currentQuery().isBlank()) {
            onSearchActiveChange(false)
        }
    }

    private fun exit() {
        uiState = uiState.synchronize(false, currentQuery())
        onExpandBars()
        onSearchActiveChange(false)
    }
}

@Composable
internal fun rememberVaultSearchStateHolder(
    searchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onExpandBars: () -> Unit,
): VaultSearchStateHolder {
    val currentQuery by rememberUpdatedState(query)
    val currentSearchActive by rememberUpdatedState(searchActive)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearchActiveChange by rememberUpdatedState(onSearchActiveChange)
    val currentOnExpandBars by rememberUpdatedState(onExpandBars)

    return remember {
        VaultSearchStateHolder(
            initialState = VaultSearchState.initial(searchActive, query),
            initialSearchActive = searchActive,
            currentQuery = { currentQuery },
            currentSearchActive = { currentSearchActive },
            onQueryChange = { currentOnQueryChange(it) },
            onSearchActiveChange = { currentOnSearchActiveChange(it) },
            onExpandBars = { currentOnExpandBars() },
        )
    }
}
