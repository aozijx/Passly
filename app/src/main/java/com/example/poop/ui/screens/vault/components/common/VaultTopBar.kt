package com.example.poop.ui.screens.vault.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.poop.ui.screens.vault.VaultTab
import com.example.poop.ui.screens.vault.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    viewModel: VaultViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val focusManager = LocalFocusManager.current

    // 本地状态：控制是否显示分类子菜单
    var showCategorySubMenu by remember { mutableStateOf(false) }

    // 当整个“更多”菜单关闭时，重置子菜单状态
    LaunchedEffect(viewModel.isMoreMenuExpanded) {
        if (!viewModel.isMoreMenuExpanded) {
            showCategorySubMenu = false
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (viewModel.isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索密码项...") },
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
                    Text(
                        text = if (selectedCategory != null) "分类: $selectedCategory" else "安全保险箱",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.toggleSearch(!viewModel.isSearchActive) }) {
                    Icon(
                        if (viewModel.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = if (viewModel.isSearchActive) "返回" else "搜索"
                    )
                }
            },
            actions = {
                if (!viewModel.isSearchActive) {
                    Box {
                        IconButton(onClick = { viewModel.toggleMoreMenu(true) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = viewModel.isMoreMenuExpanded,
                            onDismissRequest = { viewModel.toggleMoreMenu(false) }
                        ) {
                            if (!showCategorySubMenu) {
                                // --- 主菜单界面 ---
                                DropdownMenuItem(
                                    text = { Text("分类筛选") },
                                    onClick = { showCategorySubMenu = true },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (viewModel.showTOTPCode) "隐藏验证码" else "显示验证码") },
                                    onClick = {
                                        viewModel.showTOTPCode = !viewModel.showTOTPCode
                                        viewModel.toggleMoreMenu(false)
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            if (viewModel.showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility, 
                                            null
                                        ) 
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("导出备份") },
                                    onClick = {
                                        viewModel.toggleMoreMenu(false)
                                        onExportClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("导入恢复") },
                                    onClick = {
                                        viewModel.toggleMoreMenu(false)
                                        onImportClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                            } else {
                                // --- 分类子菜单界面 ---
                                DropdownMenuItem(
                                    text = { Text("返回") },
                                    onClick = { showCategorySubMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("全部分类") },
                                    onClick = {
                                        viewModel.onCategorySelect(null)
                                        viewModel.toggleMoreMenu(false)
                                    },
                                    trailingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, null) }
                                )

                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            viewModel.onCategorySelect(category)
                                            viewModel.toggleMoreMenu(false)
                                        },
                                        trailingIcon = { if (selectedCategory == category) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        )

        AnimatedVisibility(
            visible = !viewModel.isSearchActive && selectedCategory == null && scrollBehavior.state.collapsedFraction < 0.5f,
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
                        onClick = { viewModel.onTabSelect(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    VaultTab.ALL -> "全部"
                                    VaultTab.PASSWORDS -> "密码"
                                    VaultTab.TOTP -> "2FA"
                                },
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
