package com.example.poop.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.poop.R
import com.example.poop.core.common.VaultTab
import com.example.poop.features.vault.VaultViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    vaultViewModel: VaultViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    isStatusBarAutoHide: Boolean = false,
    isTopBarCollapsible: Boolean = true,
    isTabBarCollapsible: Boolean = true
) {
    val density = LocalDensity.current
    val searchQuery by vaultViewModel.searchQuery.collectAsState()
    val selectedCategory by vaultViewModel.selectedCategory.collectAsState()
    val availableCategories by vaultViewModel.availableCategories.collectAsState()
    val selectedTab by vaultViewModel.selectedTab.collectAsState()
    val focusManager = LocalFocusManager.current

    var showCategorySubMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // 关键点：如果标题栏不折叠，但标签栏或状态栏需要折叠，我们需要手动设置滚动上限
    // 否则 scrollBehavior.state.collapsedFraction 永远是 0，导致相关功能无法驱动
    LaunchedEffect(isTopBarCollapsible, isTabBarCollapsible, isStatusBarAutoHide) {
        if (!isTopBarCollapsible && (isTabBarCollapsible || isStatusBarAutoHide)) {
            // 设置一个约 64dp 的滚动上限，用于驱动折叠逻辑
            scrollBehavior.state.heightOffsetLimit = with(density) { -64.dp.toPx() }
        }
    }

    LaunchedEffect(vaultViewModel.isMoreMenuExpanded) {
        if (!vaultViewModel.isMoreMenuExpanded) showCategorySubMenu = false
    }

    LaunchedEffect(vaultViewModel.isSearchActive) {
        if (vaultViewModel.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column {
        CenterAlignedTopAppBar(
            // 如果标题栏不折叠，传入 null，TopAppBar 将固定在顶部
            scrollBehavior = if (isTopBarCollapsible) scrollBehavior else null,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (vaultViewModel.isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { vaultViewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.vault_search_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCategory != null) stringResource(R.string.vault_title_category, selectedCategory!!) 
                                   else stringResource(R.string.vault_title_default),
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategory != null) {
                            IconButton(onClick = { (vaultViewModel.selectedCategory as MutableStateFlow).value = null }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.vault_clear_filter), modifier = Modifier.padding(start = 4.dp))
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
                        IconButton(onClick = { vaultViewModel.isMoreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = vaultViewModel.isMoreMenuExpanded,
                            onDismissRequest = { vaultViewModel.isMoreMenuExpanded = false }
                        ) {
                            if (!showCategorySubMenu) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_filter)) },
                                    onClick = { showCategorySubMenu = true },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (vaultViewModel.showTOTPCode) R.string.vault_menu_hide_totp else R.string.vault_menu_show_totp)) },
                                    onClick = { vaultViewModel.showTOTPCode = !vaultViewModel.showTOTPCode; vaultViewModel.isMoreMenuExpanded = false },
                                    leadingIcon = { Icon(if (vaultViewModel.showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_settings)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onSettingsClick() },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_export)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onExportClick() },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_import)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onImportClick() },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_back)) },
                                    onClick = { showCategorySubMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_all_categories)) },
                                    onClick = { (vaultViewModel.selectedCategory as MutableStateFlow).value = null; vaultViewModel.isMoreMenuExpanded = false },
                                    trailingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, null) }
                                )
                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = { (vaultViewModel.selectedCategory as MutableStateFlow).value = category; vaultViewModel.isMoreMenuExpanded = false },
                                        trailingIcon = { if (selectedCategory == category) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vaultViewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.vault_clear_filter))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        )

        // 分类标签栏
        AnimatedVisibility(
            visible = !vaultViewModel.isSearchActive && selectedCategory == null && 
                     (!isTabBarCollapsible || scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab.ordinal),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                VaultTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { (vaultViewModel.selectedTab as MutableStateFlow).value = tab },
                        text = {
                            Text(
                                stringResource(when (tab) {
                                    VaultTab.ALL -> R.string.vault_tab_all
                                    VaultTab.PASSWORDS -> R.string.vault_tab_passwords
                                    VaultTab.TOTP -> R.string.vault_tab_totp
                                }),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
}
