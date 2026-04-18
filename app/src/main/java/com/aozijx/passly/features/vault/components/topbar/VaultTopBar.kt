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
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.components.topbar.components.VaultDropdownMenu
import com.aozijx.passly.features.vault.components.topbar.components.VaultSearchBar
import com.aozijx.passly.features.vault.components.topbar.components.VaultTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    vaultViewModel: VaultViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    currentPageIndex: Int,
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

    // 使用 collectAsStateWithLifecycle 提升性能
    val searchQuery by vaultViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vaultViewModel.selectedCategory.collectAsStateWithLifecycle()
    val availableCategories by vaultViewModel.availableCategories.collectAsStateWithLifecycle()
    val selectedTab by vaultViewModel.selectedTab.collectAsStateWithLifecycle()
    val visibleTabs by vaultViewModel.visibleTabs.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    LifecycleResumeEffect(Unit) {
        vaultViewModel.updateAutofillStatus()
        onPauseOrDispose { }
    }

    // 状态栏与标题栏折叠协调逻辑
    LaunchedEffect(isTopBarCollapsible, isTabBarCollapsible, isStatusBarAutoHide) {
        if (!isTopBarCollapsible && (isTabBarCollapsible || isStatusBarAutoHide)) {
            scrollBehavior.state.heightOffsetLimit = with(density) { -64.dp.toPx() }
        }
    }

    LaunchedEffect(vaultViewModel.isSearchActive) {
        if (vaultViewModel.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = if (isTopBarCollapsible) scrollBehavior else null,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (vaultViewModel.isSearchActive) {
                    VaultSearchBar(
                        query = searchQuery,
                        onQueryChange = { vaultViewModel.onSearchQueryChange(it) },
                        focusRequester = focusRequester,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCategory != null) stringResource(
                                R.string.vault_title_category, selectedCategory!!
                            )
                            else stringResource(R.string.vault_title_default),
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategory != null) {
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
                IconButton(onClick = { vaultViewModel.toggleSearch(!vaultViewModel.isSearchActive) }) {
                    Icon(
                        if (vaultViewModel.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = stringResource(if (vaultViewModel.isSearchActive) R.string.action_back else R.string.action_search)
                    )
                }
            },
            actions = {
                if (!vaultViewModel.isSearchActive) {
                    Box {
                        IconButton(onClick = { vaultViewModel.expandMoreMenu(true) }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more)
                            )
                        }
                        VaultDropdownMenu(
                            expanded = vaultViewModel.isMoreMenuExpanded,
                            onDismissRequest = { vaultViewModel.expandMoreMenu(false) },
                            showTOTPCode = vaultViewModel.showTOTPCode,
                            onToggleTotpVisibility = {
                                vaultViewModel.showTOTPCode = !vaultViewModel.showTOTPCode
                            },
                            isAutofillEnabled = vaultViewModel.isAutofillEnabled,
                            onEnableAutofillClick = { vaultViewModel.openAutofillSettings(context) },
                            onSettingsClick = onSettingsClick,
                            onExportClick = onExportClick,
                            onOpenPlainExport = onPlainJsonExportClick,
                            onImportClick = onImportClick,
                            availableCategories = availableCategories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { vaultViewModel.setSelectedCategory(it) })
                    }
                }
            })

        AnimatedVisibility(
            visible = visibleTabs.size > 1 && !vaultViewModel.isSearchActive && selectedCategory == null && (!isTabBarCollapsible || scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VaultTabRow(
                tabs = visibleTabs,
                selectedTab = selectedTab,
                currentPageIndex = currentPageIndex,
                onTabSelected = { vaultViewModel.selectTab(it) })
        }
    }
}