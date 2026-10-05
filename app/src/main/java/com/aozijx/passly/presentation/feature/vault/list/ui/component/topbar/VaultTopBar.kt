package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchStateHolder
import com.aozijx.passly.presentation.shared.components.topbar.passlyCompactTopAppBarColors
import com.aozijx.passly.presentation.shared.components.topbar.topAppBarContainerColor

@Composable
fun VaultTopBar(
    uiState: VaultTopBarUiState,
    searchStateHolder: VaultSearchStateHolder,
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (VaultUiAction) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val motionScheme = MaterialTheme.motionScheme
    val topBarContainerColor = topAppBarContainerColor(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrollProgress = scrollBehavior.state.overlappedFraction,
    )

    LaunchedEffect(
        uiState.collapseOnScroll,
        uiState.collapseQuickFilterOnScroll,
        uiState.hideSystemBars,
    ) {
        if (!uiState.collapseOnScroll &&
            (uiState.collapseQuickFilterOnScroll || uiState.hideSystemBars)
        ) {
            scrollBehavior.state.heightOffsetLimit = with(density) { -64.dp.toPx() }
        }
    }

    Column(
        modifier = modifier.animateContentSize(
            animationSpec = motionScheme.defaultEffectsSpec(),
        ),
    ) {
        TopAppBar(
            modifier = Modifier.background(topBarContainerColor),
            scrollBehavior = if (uiState.collapseOnScroll && !searchStateHolder.uiState.isEditing) {
                scrollBehavior
            } else {
                null
            },
            windowInsets = WindowInsets.statusBars,
            colors = passlyCompactTopAppBarColors(),
            title = {
                VaultSearchHeader(
                    uiState = uiState,
                    searchStateHolder = searchStateHolder,
                    onAction = onAction,
                    onSettingsClick = onSettingsClick,
                )
            },
        )
        uiState.menu.selectedCategory?.takeIf(String::isNotBlank)?.let { category ->
            InputChip(
                selected = true,
                onClick = { onAction(VaultUiAction.ClearCategory) },
                label = { Text(category) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.vault_clear_filter),
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
