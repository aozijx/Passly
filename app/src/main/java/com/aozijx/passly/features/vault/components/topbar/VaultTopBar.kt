package com.aozijx.passly.features.vault.components.topbar

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.components.topbar.components.VaultDropdownMenu
import com.aozijx.passly.features.vault.components.topbar.components.VaultSearchBar
import com.aozijx.passly.features.vault.components.topbar.components.VaultTabRow
import com.aozijx.passly.features.vault.model.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    vaultViewModel: VaultViewModel,
    uiState: VaultUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onPlainJsonExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    isStatusBarAutoHide: Boolean = false,
    isTopBarCollapsible: Boolean = true,
    isTabBarCollapsible: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    LifecycleResumeEffect(vaultViewModel) {
        onPauseOrDispose {
            vaultViewModel.expandMoreMenu(false)
        }
    }

    val focusRequester = remember { FocusRequester() }

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
                        onQueryChange = { vaultViewModel.onSearchQueryChange(it) },
                        focusRequester = focusRequester,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.selectedCategory != null) stringResource(
                                R.string.vault_title_category, uiState.selectedCategory
                            )
                            else stringResource(R.string.vault_title_default),
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.selectedCategory != null) {
                            IconButton(onClick = { vaultViewModel.clearSelectedCategory() }) {
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
                IconButton(onClick = { vaultViewModel.toggleSearch(!uiState.isSearchActive) }) {
                    Icon(
                        if (uiState.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = stringResource(if (uiState.isSearchActive) R.string.action_back else R.string.action_search)
                    )
                }
            },
            actions = {
                if (!uiState.isSearchActive) {
                    Box {
                        IconButton(onClick = { vaultViewModel.expandMoreMenu(true) }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more)
                            )
                        }
                        VaultDropdownMenu(
                            expanded = uiState.isMoreMenuExpanded,
                            onDismissRequest = { vaultViewModel.expandMoreMenu(false) },
                            showTOTPCode = uiState.showTOTPCode,
                            onToggleTotpVisibility = {
                                vaultViewModel.toggleShowTOTPCode()
                            },
                            isAutofillEnabled = vaultViewModel.isAutofillEnabled,
                            onEnableAutofillClick = { vaultViewModel.openAutofillSettings(context) },
                            onSettingsClick = onSettingsClick,
                            onExportClick = onExportClick,
                            onOpenPlainExport = onPlainJsonExportClick,
                            onImportClick = onImportClick,
                            availableCategories = uiState.availableCategories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { vaultViewModel.setSelectedCategory(it) })
                    }
                }
            })

        AnimatedVisibility(
            visible = uiState.visibleTabs.size > 1 && !uiState.isSearchActive && uiState.selectedCategory == null && (!isTabBarCollapsible || scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VaultTabRow(
                tabs = uiState.visibleTabs,
                selectedTabIndex = uiState.visibleTabs.indexOf(uiState.selectedTab)
                    .coerceAtLeast(0),
                onTabSelected = { index ->
                    uiState.visibleTabs.getOrNull(index)?.let { vaultViewModel.selectTab(it) }
                }
            )
        }
    }
}