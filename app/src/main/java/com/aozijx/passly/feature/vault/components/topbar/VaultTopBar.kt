package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.VaultTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    uiState: VaultUiState,
    selectedTabIndex: Int,
    maxTabsWithoutScroll: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingsClick: () -> Unit = {},
    isStatusBarAutoHide: Boolean = false,
    isTopBarCollapsible: Boolean = true,
    isTabBarCollapsible: Boolean = true,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onClearEntryType: () -> Unit,
    onToggleTotpVisibility: () -> Unit,
    onEntryTypeSelected: (String?) -> Unit,
    onSortSelected: (VaultSortSpec) -> Unit,
    onSelectTab: (VaultTab) -> Unit
) {
    val density = LocalDensity.current
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var navigateToSettingsAfterDismiss by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            isMoreMenuExpanded = false
            navigateToSettingsAfterDismiss = false
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(navigateToSettingsAfterDismiss, isMoreMenuExpanded) {
        if (navigateToSettingsAfterDismiss && !isMoreMenuExpanded) {
            navigateToSettingsAfterDismiss = false
            onSettingsClick()
        }
    }

    LaunchedEffect(isTopBarCollapsible, isTabBarCollapsible, isStatusBarAutoHide) {
        if (!isTopBarCollapsible && (isTabBarCollapsible || isStatusBarAutoHide)) {
            scrollBehavior.state.heightOffsetLimit = with(density) { -64.dp.toPx() }
        }
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = if (isTopBarCollapsible) scrollBehavior else null,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (uiState.isSearchActive) {
                    VaultSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        focusRequester = focusRequester,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.selectedEntryTypeName != null) stringResource(
                                R.string.vault_title_entry_type,
                                EntryType.fromName(uiState.selectedEntryTypeName).displayName
                            )
                            else stringResource(R.string.vault_title_default),
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.selectedEntryTypeName != null) {
                            IconButton(onClick = onClearEntryType) {
                                Icon(
                                    Icons.Default.Clear,
                                    stringResource(R.string.vault_clear_filter),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onToggleSearch(!uiState.isSearchActive) }) {
                    Icon(
                        if (uiState.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = stringResource(if (uiState.isSearchActive) R.string.back else R.string.search)
                    )
                }
            },
            actions = {
                if (!uiState.isSearchActive) {
                    Box {
                        IconButton(onClick = { isMoreMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }
                        if (isMoreMenuExpanded) {
                            VaultDropdownMenu(
                                onDismissRequest = { isMoreMenuExpanded = false },
                                showTOTPCode = uiState.showTOTPCode,
                                onToggleTotpVisibility = onToggleTotpVisibility,
                                onSettingsClick = {
                                    navigateToSettingsAfterDismiss = true
                                },
                                availableEntryTypes = uiState.availableEntryTypes,
                                selectedEntryTypeName = uiState.selectedEntryTypeName,
                                onEntryTypeSelected = onEntryTypeSelected,
                                selectedSort = uiState.selectedSort,
                                onSortSelected = onSortSelected
                            )
                        }
                    }
                }
            })

        AnimatedVisibility(
            visible = uiState.visibleTabs.size > 1 && !uiState.isSearchActive && uiState.selectedEntryTypeName == null && (!isTabBarCollapsible || scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VaultTabRow(
                tabs = uiState.visibleTabs,
                selectedTabIndex = selectedTabIndex,
                maxTabsWithoutScroll = maxTabsWithoutScroll,
                onTabSelected = { index ->
                    uiState.visibleTabs.getOrNull(index)?.let { onSelectTab(it) }
                }
            )
        }
    }
}
