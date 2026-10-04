package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchStateHolder

@Composable
internal fun VaultSearchHeader(
    uiState: VaultTopBarUiState,
    searchStateHolder: VaultSearchStateHolder,
    onAction: (VaultUiAction) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val expansionProgress by animateFloatAsState(
        targetValue = searchStateHolder.uiState.layoutProgress,
        animationSpec = if (searchStateHolder.uiState.isDirectManipulation) {
            snap()
        } else {
            motionScheme.defaultEffectsSpec()
        },
        label = "VaultSearchExpansion",
    )
    val widthFraction = 0.94f + (0.06f * expansionProgress.coerceIn(0f, 1f))

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxWidth(widthFraction)) {
            VaultSearchBar(
                query = uiState.query,
                isEditing = searchStateHolder.uiState.isEditing,
                onQueryChange = searchStateHolder::queryChanged,
                onSearch = searchStateHolder::submit,
                onFocusChanged = searchStateHolder::focusChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            VaultSearchAction(
                uiState = uiState,
                searchStateHolder = searchStateHolder,
                onAction = onAction,
                onSettingsClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
